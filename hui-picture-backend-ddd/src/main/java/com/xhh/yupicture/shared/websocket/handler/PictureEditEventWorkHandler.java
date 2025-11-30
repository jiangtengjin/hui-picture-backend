package com.xhh.yupicture.shared.websocket.handler;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.xhh.yupicture.domain.service.UserDomainService;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.shared.websocket.disruptor.PictureEditEvent;
import com.xhh.yupicture.shared.websocket.model.PictureEditRequestMessage;
import com.xhh.yupicture.shared.websocket.model.PictureEditResponseMessage;
import com.xhh.yupicture.shared.websocket.model.enums.PictureEditMessageTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 定义事件处理器（消费者）
 * 将不同类型的消息分发到对应的处理器
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserDomainService userDomainService;

    @Override
    public void onEvent(PictureEditEvent event) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();
        // 获取消息类型
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        // 调用对应的消息处理方法
        switch (messageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(session, pictureEditRequestMessage, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(session, pictureEditRequestMessage, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(session, pictureEditRequestMessage, user, pictureId);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("无效的消息类型");
                pictureEditResponseMessage.setUser(userDomainService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}
