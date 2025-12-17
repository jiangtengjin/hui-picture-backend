package com.xhh.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum TaskStatusEnum {

    UNKNOWN("UNKNOWN", "任务不存在", 0),
    SUCCEEDED("SUCCEEDED","任务执行成功", 1),
    FAILED("FAILED", "任务执行失败", -1),
    RUNNING("RUNNING", "任务处理中", 2),
    PENDING("PENDING", "任务排队中", 3),
    CANCELED("CANCELED", "任务已取消", 4);

    private final String text;

    private final String desc;

    private final int value;

    TaskStatusEnum(String text, String desc, int value) {
        this.text = text;
        this.desc = desc;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param text  枚举值的 text
     * @return      枚举值
     */
    public static TaskStatusEnum getEnumByText(String text) {
        if (ObjUtil.isEmpty(text)) {
            return null;
        }
        for (TaskStatusEnum taskStatusEnum : TaskStatusEnum.values()) {
            if (taskStatusEnum.text.equals(text)) {
                return taskStatusEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return 文本列表
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(TaskStatusEnum.values())
                .map(TaskStatusEnum::getText)
                .collect(Collectors.toList());
    }

}
