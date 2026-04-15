package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.ImageGenerateVideoRequest;
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

    void recordUsageAsync(Long userId, String module, String aiMode, BigDecimal totalPrice);
}
