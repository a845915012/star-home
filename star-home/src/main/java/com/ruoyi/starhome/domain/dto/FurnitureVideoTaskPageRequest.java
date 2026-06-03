package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频任务分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FurnitureVideoTaskPageRequest extends BasePageRequest {
    @Schema(description = "请求时描述(模糊匹配)", example = "现代客厅")
    private String prompt;
}
