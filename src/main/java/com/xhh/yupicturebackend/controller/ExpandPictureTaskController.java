package com.xhh.yupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhh.yupicturebackend.common.BaseResponse;
import com.xhh.yupicturebackend.common.DeleteRequest;
import com.xhh.yupicturebackend.common.ResultUtils;
import com.xhh.yupicturebackend.exception.ErrorCode;
import com.xhh.yupicturebackend.exception.ThrowUtils;
import com.xhh.yupicturebackend.model.dto.TaskQueryRequest;
import com.xhh.yupicturebackend.model.entity.ExpandPictureTask;
import com.xhh.yupicturebackend.model.vo.TaskVO;
import com.xhh.yupicturebackend.service.ExpandPictureTaskService;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 根据 id 查询任务
     *
     * @param id
     * @return
     */
    @GetMapping("get/{id}")
    public BaseResponse<TaskVO> getTaskById(@PathVariable Long id){
        ExpandPictureTask task = expandPictureTaskService.getById(id);
        return ResultUtils.success(TaskVO.objToVo(task));
    }

    /**
     * 根据 id 删除任务
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("delete")
    public BaseResponse<Boolean> deleteTaskById(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id < 1, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(expandPictureTaskService.removeById(id));
    }

}
