package com.yupi.yupicturebackend.manager.websocket.config;

import com.yupi.yupicturebackend.manager.websocket.handler.PictureEditHandler;
import com.yupi.yupicturebackend.manager.websocket.interceptor.WsHandShakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private WsHandShakeInterceptor wsHandShakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandler, "/ws/picture/edit") // 为指定路径配置处理器
                .addInterceptors(wsHandShakeInterceptor) // 拦截器
                .setAllowedOrigins("*");
    }
}
