package com.xhh.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum TaskStatusEnum {

    UNKNOWN("任务不存在", 0),
    SUCCEEDED("任务执行成功", 1),
    FAILED("任务执行失败", -1),
    RUNNING("任务处理中", 2),
    PENDING("任务排队中", 3),
    CANCELED("任务已取消", 4);

    private final String text;

    private final int value;

    TaskStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static TaskStatusEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (TaskStatusEnum taskStatusEnum : TaskStatusEnum.values()) {
            if (taskStatusEnum.value == value) {
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
