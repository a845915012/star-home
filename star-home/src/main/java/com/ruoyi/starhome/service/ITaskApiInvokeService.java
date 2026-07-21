package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.GenerateSceneRequest;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoRequest;
import com.ruoyi.starhome.domain.dto.SceneResultItem;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;

public interface ITaskApiInvokeService {
    TaskApiInvokeResponse invokeTaskApi(TaskApiInvokeRequest request);

    TaskApiInvokeResponse invokeTaskApiBlocking(TaskApiInvokeRequest request);

    SseEmitter createStream(Long userId);

    TaskApiInvokeResponse invokeGeminiImageApi(TaskApiInvokeRequest request) throws IOException;

    TaskApiInvokeResponse imageGenerateVideo(ImageGenerateVideoRequest request) throws IOException;


    void completeDeferredVideoUsageRecord(Long generationTaskId, String outputFiles, String status);

    /**
     * 单任务图生图：原子扣费（扣前）→ 生成 → 失败退款。设计为在异步线程中调用，
     * userId 由调用方显式传入（异步线程无 SecurityContext）。
     *
     * @param request 单个生成请求
     * @param userId  用户ID
     * @param index   入参序号，用于结果排序/匹配
     */
    SceneResultItem generateSceneSingle(GenerateSceneRequest request, Long userId, int index);
}
