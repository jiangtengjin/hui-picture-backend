package com.xhh.yupicturebackend.manager.websocket.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xhh.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.xhh.yupicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.xhh.yupicturebackend.manager.websocket.model.SyncData;
import com.xhh.yupicturebackend.manager.websocket.model.enums.PictureEditActionEnum;
import com.xhh.yupicturebackend.manager.websocket.model.enums.PictureEditMessageTypeEnum;
import com.xhh.yupicturebackend.model.entity.User;
import com.xhh.yupicturebackend.model.vo.UserVO;
import com.xhh.yupicturebackend.service.UserService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Websocket 处理器
 * 在连接成功、连接关闭、接收到客户端消息时进行相应的处理
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    @Resource
    private ObjectMapper objectMapper;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    // 添加心跳和重连相关字段
    private final Map<String, Long> lastHeartbeatTime = new ConcurrentHashMap<>();
    private static final long HEARTBEAT_INTERVAL = 30000; // 30 秒超时
    private final ScheduledExecutorService heartbeatChecker = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init () {
        // 启动心跳检测
        heartbeatChecker.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        heartbeatChecker.shutdown();
    }

    /**
     * 检查心跳，处理超时连接
     */
    private void checkHeartbeats() {
        long currentTime = System.currentTimeMillis();
        lastHeartbeatTime.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > HEARTBEAT_INTERVAL) {
                String sessionId = entry.getKey();
                WebSocketSession session = getSessionById(sessionId);
                if (session != null) {
                    try {
                        log.warn("心跳超时，关闭连接：{}", sessionId);
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    } catch (IOException e) {
                        log.error("关闭超时连接失败", e);
                    }
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 根据 sessionId 获取 WebSocketSession
     * @param sessionId
     * @return
     */
    private WebSocketSession getSessionById(String sessionId) {
        for (Set<WebSocketSession> sessionSet : pictureSessions.values()) {
            for (WebSocketSession session : sessionSet) {
                if (session.getId().equals(sessionId)) {
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * 连接成功时调用
     * 保存会话到集合中，并给其他会话发送消息
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // 如果没有用户进入编辑状态，当前用户自动进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            this.handleEnterEditMessage(session,
                    new PictureEditRequestMessage(),
                    user, pictureId);
        } else {
            // 有人正在编辑，发送加入编辑的事件
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
            String message = String.format("%s 加入编辑", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            // 广播给同一张图片的用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }

        // 如果是后面进来的，这里就可以把当前正在编辑的用户信息给当前用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null) {
            User editingUser = userService.getById(editingUserId);
            if (ObjectUtil.isNotEmpty(editingUser)) {
                // 构建编辑消息
                String msg = String.format("用户 %s 正在编辑图片", editingUser.getUserName());
                PictureEditResponseMessage editingMsg = new PictureEditResponseMessage();
                editingMsg.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
                editingMsg.setUser(userService.getUserVO(editingUser));
                editingMsg.setMessage(msg);
                // 单独发送消息给当前用户
                String str = objectMapper.writeValueAsString(editingMsg);
                TextMessage textMessage = new TextMessage(str);
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 接收客户端消息
     * 根据不同消息类别执行不同的处理
     * 可以使用策略模式优化 -> 定义一个接口，设置不同的处理方法，分别由实现类来实现
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 pictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从 session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        // 生产消息
        pictureEditEventProducer.publishEvent(session, pictureEditRequestMessage, user, pictureId);
    }

    /**
     * 关闭客户端连接
     * 需要移除当前用户的登录状态，并且删除当前会话，通知其他用户
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        // 移除心跳
        lastHeartbeatTime.remove(session.getId());

        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 移除当前用户的登录状态
        handleExitEditMessage(session, null, user, pictureId);

        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        log.info("用户 {} 断开连接。图片 ID：{}，关闭原因：{}", user.getUserName(), pictureId, status.toString());
    }

    /**
     * 处理退出编辑的消息
     * 移除当前用户的编辑状态，并通知其他用户
     *
     * @param session
     * @param pictureEditRequestMessage
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(WebSocketSession session,
                                      PictureEditRequestMessage pictureEditRequestMessage,
                                      User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            // 向其他客户端发送消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s 退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理正在执行编辑操作的消息
     * 需要把消息同步给 除自己之外的所有用户
     *
     * @param session
     * @param pictureEditRequestMessage
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(WebSocketSession session,
                                        PictureEditRequestMessage pictureEditRequestMessage,
                                        User user,
                                        Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 向其他客户端发送消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s 执行 %s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除自己之外的所有用户，否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理进入编辑的消息
     * 设置当前用户为编辑用户，并向其他客户端发送消息
     *
     * @param session
     * @param pictureEditRequestMessage
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(WebSocketSession session,
                                        PictureEditRequestMessage pictureEditRequestMessage,
                                        User user,
                                        Long pictureId) throws Exception {
        // 没有用户正在编辑图片才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());
            // 向其他客户端发送消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s 开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理心跳消息
     *
     * @param session
     */
    public void handleHeartbeatMessage(WebSocketSession session) {
        lastHeartbeatTime.put(session.getId(), System.currentTimeMillis());

        // 发送心跳响应
        try {
            PictureEditResponseMessage response = new PictureEditResponseMessage();
            response.setType(PictureEditMessageTypeEnum.HEART_BEAT.getValue());
            response.setTimestamp(System.currentTimeMillis());
            sendMessageToSession(session, response);
        } catch (Exception e) {
            log.error("发送心跳响应失败", e);
        }
    }

    /**
     * 处理同步请求
     *
     * @param session
     * @param request
     * @throws Exception
     */
    public void handleSyncRequestMessage(WebSocketSession session, PictureEditRequestMessage request) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");

        // 构建同步数据
        PictureEditResponseMessage syncResponse = new PictureEditResponseMessage();
        syncResponse.setType("sync_data");
        syncResponse.setTimestamp(System.currentTimeMillis());

        // 同步数据应该包含：
        // 1. 当前编辑状态
        // 2. 最近的操作历史
        // 3. 当前在线用户
        SyncData syncData = buildSyncData(pictureId, user);
        syncResponse.setData(syncData);

        sendMessageToSession(session, syncResponse);

        log.info("向用户 {} 发送同步数据，图片ID: {}", user.getUserName(), pictureId);
    }

    /**
     * 构建同步数据
     */
    private SyncData buildSyncData(Long pictureId, User user) {
        SyncData syncData = new SyncData();

        // 当前编辑者
        Long editingUserId = pictureEditingUsers.get(pictureId);
        syncData.setCurrentEditor(editingUserId);

        // 在线用户列表
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessions)) {
            List<UserVO> onlineUsers = sessions.stream()
                    .map(s -> (User) s.getAttributes().get("user"))
                    .filter(Objects::nonNull)
                    .map(userService::getUserVO)
                    .collect(Collectors.toList());
            syncData.setOnlineUsers(onlineUsers);
        }

        // 这里可以添加操作历史等更多同步数据
        syncData.setLastSyncTime(System.currentTimeMillis());

        return syncData;
    }

    /**
     * 向单个会话发送消息
     *
     * @param session       消息接收者
     * @param message      消息内容
     */
    private void sendMessageToSession(WebSocketSession session, PictureEditResponseMessage message) throws Exception {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }

    /**
     * 广播消息给所有协作者(不包括自己)
     *
     * @param pictureEditResponseMessage
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     * @Param pictureId
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播消息给所有协作者（包括自己）
     *
     * @param pictureEditResponseMessage
     * @param pictureId
     * @param pictureEditResponseMessage
     * @Param pictureId
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

}