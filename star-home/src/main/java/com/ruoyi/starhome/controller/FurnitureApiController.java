package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.framework.security.util.SecurityFrameworkUtils;
import com.ruoyi.starhome.domain.dto.CopyGenerateRequest;
import com.ruoyi.starhome.domain.dto.GenerateSceneRequest;
import com.ruoyi.starhome.domain.dto.ImageGenerateVideoClientRequest;
import com.ruoyi.starhome.service.IFurnitureApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static com.ruoyi.common.core.domain.AjaxResult.success;

@Tag(name = "星链家-AI服务调用接口")
@RestController
@RequestMapping("/api")
public class FurnitureApiController {

    @Autowired
    private IFurnitureApiService furnitureApiService;

    @Operation(summary = "场景图片生成接口", description = "图生图接口")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "场景图片生成接口"
    )
    @Log(title = "场景图片生成", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/image/generate-scene", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AjaxResult imageGenerateScene(@RequestBody GenerateSceneRequest request) throws IOException {
        return success(furnitureApiService.imageGenerateScene(request));
    }

    @Operation(summary = "文案生成接口", description = "文案生成接口")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "文案生成接口"
    )
    @Log(title = "文案生成接口", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/copy/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AjaxResult copyGenerate(@RequestBody CopyGenerateRequest request) {
        return success(furnitureApiService.copyGenerate(request));
    }

    @Operation(summary = "图像生成视频", description = "图像生成视频")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "图像生成视频"
    )
    @Log(title = "图像生成视频", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/image/generate-video", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AjaxResult imageGenerateVideo(@RequestBody ImageGenerateVideoClientRequest request) throws IOException {
        return success(furnitureApiService.imageGenerateVideo(request));
    }

    @Operation(summary = "建立任务SSE流", description = "建立任务调用的SSE连接")
    @GetMapping(value = "/build/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createStream() {
        return furnitureApiService.createStream();
    }
}
