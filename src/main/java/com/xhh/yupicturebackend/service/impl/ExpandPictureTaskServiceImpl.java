package com.xhh.yupicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xhh.yupicturebackend.exception.BusinessException;
import com.xhh.yupicturebackend.exception.ErrorCode;
import com.xhh.yupicturebackend.mapper.ExpandPictureTaskMapper;
import com.xhh.yupicturebackend.model.entity.ExpandPictureTask;
import com.xhh.yupicturebackend.model.entity.User;
import com.xhh.yupicturebackend.model.enums.TaskStatusEnum;
import com.xhh.yupicturebackend.service.ExpandPictureTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
* @author 机hui难得
* @description 针对表【expand_picture_task(AI 扩图任务表)】的数据库操作Service实现
* @createDate 2025-12-15 21:23:07
*/
@Slf4j
@Service
public class ExpandPictureTaskServiceImpl extends ServiceImpl<ExpandPictureTaskMapper, ExpandPictureTask>
    implements ExpandPictureTaskService{

    @Override
    public Boolean createTask(CreateOutPaintingTaskResponse response, User loginUser) {
        ExpandPictureTask task = new ExpandPictureTask();
        task.setTaskId(response.getOutput().getTaskId());
        String taskStatus = response.getOutput().getTaskStatus();
        TaskStatusEnum taskStatusEnum = TaskStatusEnum.getEnumByText(taskStatus);
        if (taskStatusEnum == null) {
            log.error("任务状态错误，taskStatus: {}", taskStatus);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务状态错误");
        }
        task.setTaskStatus(taskStatusEnum.getValue());
        task.setRequestId(response.getRequestId());
        String code = response.getCode();
        if (StrUtil.isNotBlank(code)) {
            task.setCode(code);
        }
        String message = response.getMessage();
        if (StrUtil.isNotBlank(message)) {
            task.setMessage(message);
        }
        task.setUserId(loginUser.getId());

        return this.save(task);
    }
}




