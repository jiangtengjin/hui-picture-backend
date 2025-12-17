package com.xhh.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xhh.yupicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.xhh.yupicturebackend.model.dto.TaskQueryRequest;
import com.xhh.yupicturebackend.model.entity.ExpandPictureTask;
import com.xhh.yupicturebackend.model.entity.User;
import com.xhh.yupicturebackend.model.vo.TaskVO;

/**
* @author 机hui难得
* @description 针对表【expand_picture_task(AI 扩图任务表)】的数据库操作Service
* @createDate 2025-12-15 21:23:07
*/
public interface ExpandPictureTaskService extends IService<ExpandPictureTask> {

    /**
     * 添加任务记录
     *
     * @param response          阿里云 AI 创建扩图任务响应结果
     * @param loginUser         当前登录用户
     * @return
     */
    Boolean createTask(CreateOutPaintingTaskResponse response, User loginUser);

    /**
     * 更新任务记录
     *
     * @param response      AI 扩图任务结果
     */
    void updateTask(GetOutPaintingTaskResponse response);

    /**
     * 分页获取任务记录
     *
     * @param taskQueryRequest
     * @return
     */
    Page<TaskVO> getTaskList(TaskQueryRequest taskQueryRequest);

    /**
     * 获取 QueryWrapper 对象
     *
     * @param taskQueryRequest
     * @return
     */
    QueryWrapper<ExpandPictureTask> getQueryWrapper(TaskQueryRequest taskQueryRequest);

}
