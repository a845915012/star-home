package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ImageGenerateVideoRequest {
    /**
     * 可选：请求上下文无登录态（如定时任务）时用于透传用户ID。
     */
    private Long userId;
    private String product;
    private String material;
    /**
     * 必须是外部可访问的url
     */
    private String imageUrl;
    private String prompt;
    private Long generationTaskId;
    private String consumeCode;
    private BigDecimal consumePrice;
}
