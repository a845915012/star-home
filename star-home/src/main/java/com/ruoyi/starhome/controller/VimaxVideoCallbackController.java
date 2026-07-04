package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.starhome.domain.dto.VimaxVideoCallbackRequest;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * vimax-agent 视频任务回调接口，替代原有轮询模式。
 * 无鉴权，异常时返回 500 供 vimax-agent 重试，正常返回 200。
 */
@Tag(name = "vimax-agent 回调")
@RestController
@RequestMapping("/starhome/vimax/callback")
@Slf4j
public class VimaxVideoCallbackController {

    @Autowired
    private IFurnitureVideoTaskService furnitureVideoTaskService;

    @Operation(summary = "视频任务结果回调")
    @PostMapping("/video")
    @Anonymous
    public R<?> onVideoTaskComplete(@RequestBody VimaxVideoCallbackRequest request) {
        furnitureVideoTaskService.handleVideoTaskCallback(request);
        return R.ok();
    }
}
