package com.xhh.yupicturebackend.manager.websocket.model;

import com.xhh.yupicturebackend.model.vo.UserVO;
import lombok.Data;

import java.util.List;

// 同步数据实体类
@Data
public class SyncData {
    private Long currentEditor;
    private List<UserVO> onlineUsers;
    private Long lastSyncTime;
    // 可以根据需要添加更多同步字段
    private Object editHistory;
    private Object currentState;
}