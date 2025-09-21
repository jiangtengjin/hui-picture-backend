package com.xhh.yupicturebackend.api.aliyun;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import com.xhh.yupicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xhh.yupicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.xhh.yupicturebackend.exception.BusinessException;
import com.xhh.yupicturebackend.exception.ErrorCode;
import com.xhh.yupicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunApi {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态地址
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 创建任务
    // curl --location --request POST 'https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting' \
    //--header "Authorization: Bearer $DASHSCOPE_API_KEY" \
    //--header 'X-DashScope-Async: enable' \
    //--header 'Content-Type: application/json' \
    //--data '{
    //    "model": "image-out-painting",
    //    "input": {
    //        "image_url": "http://xxx/image.jpg"
    //    },
    //    "parameters":{
    //        "angle": 45,
    //        "x_scale":1.5,
    //        "y_scale":1.5
    //    }
    //}'

    /**
     * 创建任务
     *
     * @param request
     * @return
     */
    public CreateOutPaintingTaskResponse createTask(CreateOutPaintingTaskRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.OPERATION_ERROR, "扩图参数为空");
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理，设置为 enable
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(request));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常: {}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (errorCode != null) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败, errorCode: {}, message: {}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId 任务 id
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }

        try (HttpResponse response = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(response.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
