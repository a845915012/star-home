package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ImageGenerateSceneRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "幂等键，由前端生成；10分钟内重复提交会被拒绝（防止前端超时重试导致重复扣费）")
    private String requestId;

    @Schema(description = "场景生成任务列表，每个元素即一次图生图任务")
    private List<GenerateSceneRequest> items;
}
