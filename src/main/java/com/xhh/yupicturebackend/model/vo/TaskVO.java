package com.xhh.yupicturebackend.model.vo;

import com.xhh.yupicturebackend.model.entity.ExpandPictureTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 扩图任务表
 *
 * @TableName expand_picture_task
 */
@Data
public class TaskVO implements Serializable {

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
     * 输出图像 url 地址
     */
    private String outputPictureUrl;

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

    /**
     * 对象转封装类
     */
    public static TaskVO objToVo(ExpandPictureTask expandPictureTask) {
        if (expandPictureTask == null) {
            return null;
        }
        TaskVO taskVO = new TaskVO();
        BeanUtils.copyProperties(expandPictureTask, taskVO);
        return taskVO;
    }
}