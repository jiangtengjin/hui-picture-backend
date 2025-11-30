package com.xhh.yupicture.shared.websocket.interceptor;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.xhh.yupicture.application.service.UserApplicationService;
import com.xhh.yupicture.domain.picture.entity.Picture;
import com.xhh.yupicture.domain.repository.PictureRepository;
import com.xhh.yupicture.domain.user.entity.User;
import com.xhh.yupicture.shared.auth.constans.SpaceUserAuthConstant;
import com.xhh.yupicture.shared.auth.SpaceUserAuthManager;
import com.xhh.yupicture.domain.space.entity.Space;
import com.xhh.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.xhh.yupicture.domain.service.SpaceDomainService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * websocket 拦截器
 * 需要在建立连接前进行权限校验
 * 校验通过后设置一些属性，比如登录用户信息、编辑的图片id等
 */
@Component
@Slf4j
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 获取请求参数
            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            User loginUser = userApplicationService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            // 校验用户是否有该图片权限
            Picture picture = pictureRepository.getById(pictureId);
            if (ObjUtil.isEmpty(picture)) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (ObjUtil.isNotEmpty(spaceId)) {
                space = spaceDomainService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("不是团队空间，拒绝握手");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserAuthConstant.PICTURE_EDIT)) {
                log.error("没有图片编辑权限，拒绝握手");
                return false;
            }
            // 设置 attributes
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureId)); // 记得转换为 Long 类型
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
