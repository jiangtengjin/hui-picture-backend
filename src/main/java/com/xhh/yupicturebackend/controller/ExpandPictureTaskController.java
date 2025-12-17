package com.xhh.yupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhh.yupicturebackend.common.BaseResponse;
import com.xhh.yupicturebackend.common.ResultUtils;
import com.xhh.yupicturebackend.exception.ErrorCode;
import com.xhh.yupicturebackend.exception.ThrowUtils;
import com.xhh.yupicturebackend.model.dto.TaskQueryRequest;
import com.xhh.yupicturebackend.model.vo.TaskVO;
import com.xhh.yupicturebackend.service.ExpandPictureTaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/image/expand/task")
public class ExpandPictureTaskController {

    @Resource
    private ExpandPictureTaskService expandPictureTaskService;

    /**
     * 分页查询扩图任务
     *
     * @param taskQueryRequest
     * @return
     */
    @PostMapping("/page")
    public BaseResponse<Page<TaskVO>> getTaskByPage(@RequestBody TaskQueryRequest taskQueryRequest){
        ThrowUtils.throwIf(taskQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<TaskVO> taskList = expandPictureTaskService.getTaskList(taskQueryRequest);
        return ResultUtils.success(taskList);
    }

}
