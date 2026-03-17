package com.ruoyi.starhome.service;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.domain.dto.CopyGenerateRequest;
import com.ruoyi.starhome.domain.dto.GenerateSceneRequest;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IFurnitureApiService {
    TaskApiInvokeResponse imageGenerateScene(GenerateSceneRequest request);
    TaskApiInvokeResponse copyGenerate(CopyGenerateRequest request);
    SseEmitter createStream();
}
