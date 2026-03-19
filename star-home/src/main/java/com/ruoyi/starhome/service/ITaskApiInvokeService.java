package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ITaskApiInvokeService {
    TaskApiInvokeResponse invokeTaskApi(TaskApiInvokeRequest request,String module);

    TaskApiInvokeResponse invokeTaskApiBlocking(TaskApiInvokeRequest request,String module);

    SseEmitter createStream(Long userId);
}
