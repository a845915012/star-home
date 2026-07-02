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
    /**
     * api池编号，用于查询token和model
     */
    private String number;
    /**
     * 卖点信息
     */
    private String sellingPoints;
    /**
     * 视频生成类型，前端传入
     */
    private String type;
    /**
     * 从furniture_number_api_pool获取的apiKey
     */
    private String token;
    /**
     * 从furniture_number_api_pool获取的mode
     */
    private String model;
}
