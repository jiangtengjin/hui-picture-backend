package com.xhh.yupicturebackend.model.dto;

import com.xhh.yupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 扩图任务表
 *
 * @TableName expand_picture_task
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 请求 id
     */
    private String requestId;

    /**
     * 任务 id
     */
    private String taskId;

    /**
     * 任务状态：0，任务不存在；1，执行成功；-1，执行失败；2，执行中；3，排队中；4，已取消
     */
    private Integer taskStatus;

    /**
     * 请求错误码
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 任务总数
     */
    private Integer total;

    /**
     * 成功数
     */
    private Integer succeeded;

    /**
     * 失败数
     */
    private Integer failed;

    /**
     * 模型生成成功的图片数
     */
    private Integer imageCount;

    /**
     * 提交时间
     */
    private Date submitTime;

    /**
     * 完成时间
     */
    private Date endTime;


    private static final long serialVersionUID = 1L;
}