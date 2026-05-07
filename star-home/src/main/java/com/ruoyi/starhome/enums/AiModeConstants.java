package com.ruoyi.starhome.enums;

import java.math.BigDecimal;

public enum AiModeConstants {
    TEXT_GENERATE_AI("gpt-4o-all"),
    IMAGE_IMAGE_AI("gemini-3-pro-image-preview"),
    IMAGE_VIDEO_AI("veo3.1-pro");

    private final String aiMode;
    AiModeConstants(String aiMode) {
        this.aiMode = aiMode;
    }
    // 提供 getter 方法获取 BigDecimal 值
    public String getAiMode() {
        return aiMode;
    }
}
