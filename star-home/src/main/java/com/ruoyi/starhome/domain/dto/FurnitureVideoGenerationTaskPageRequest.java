package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频生成头任务分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FurnitureVideoGenerationTaskPageRequest extends BasePageRequest {
    @Schema(description = "状态", example = "success")
    private String status;
}
