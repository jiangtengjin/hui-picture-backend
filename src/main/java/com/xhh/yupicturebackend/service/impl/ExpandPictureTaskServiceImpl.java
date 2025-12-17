package com.xhh.yupicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xhh.yupicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.xhh.yupicturebackend.exception.BusinessException;
import com.xhh.yupicturebackend.exception.ErrorCode;
import com.xhh.yupicturebackend.exception.ThrowUtils;
import com.xhh.yupicturebackend.mapper.ExpandPictureTaskMapper;
import com.xhh.yupicturebackend.model.entity.ExpandPictureTask;
import com.xhh.yupicturebackend.model.entity.User;
import com.xhh.yupicturebackend.model.enums.TaskStatusEnum;
import com.xhh.yupicturebackend.service.ExpandPictureTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

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

    @Override
    public void updateTask(GetOutPaintingTaskResponse response) {
        // 仅在任务状态为成功或失败时更新，因为采用轮询的方式查询任务状态，防止数据量大情况下压跨数据库
        GetOutPaintingTaskResponse.Output output = response.getOutput();
        String taskStatus = output.getTaskStatus();
        TaskStatusEnum taskStatusEnum = TaskStatusEnum.getEnumByText(taskStatus);
        if (taskStatusEnum == null) {
            log.error("任务状态错误，taskStatus: {}", taskStatus);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务状态错误");
        }
        if (taskStatusEnum != TaskStatusEnum.SUCCEEDED && taskStatusEnum != TaskStatusEnum.FAILED) {
            log.info("任务状态为 {}，不更新任务", taskStatus);
            return;
        }
        // 1. 查看任务是否存在
        LambdaQueryWrapper<ExpandPictureTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExpandPictureTask::getTaskId, response.getOutput().getTaskId());
        ExpandPictureTask task = this.getOne(queryWrapper);
        ThrowUtils.throwIf(task == null, ErrorCode.SYSTEM_ERROR, "任务不存在");

        // 2. 更新任务状态
        task.setTaskStatus(taskStatusEnum.getValue());
        // 处理时间字符串为 date 对象
        String endTimeStr = output.getEndTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime localDateTime = LocalDateTime.parse(endTimeStr, formatter);
        Date endTime = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        task.setEndTime(endTime);
        // 如果任务执行失败，则记录错误日志
        if (output.getTaskStatus().equals(TaskStatusEnum.FAILED.getText())) {
            task.setCode(output.getCode());
            task.setMessage(output.getMessage());
        }
        // 如果任务执行成功，则记录生成的图片 url
        if (output.getTaskStatus().equals(TaskStatusEnum.SUCCEEDED.getText())) {
            task.setOutputPictureUrl(output.getOutputImageUrl());
        }
        this.updateById(task);
    }
}




