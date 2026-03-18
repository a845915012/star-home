package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI调用记录分页查询请求
 */
public class FurnitureAiCallRecordsPageRequest extends BasePageRequest {
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "模块", example = "draw")
    private String module;

    @Schema(description = "AI模型", example = "gpt-4o")
    private String aiMode;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAiMode() {
        return aiMode;
    }

    public void setAiMode(String aiMode) {
        this.aiMode = aiMode;
    }
}
