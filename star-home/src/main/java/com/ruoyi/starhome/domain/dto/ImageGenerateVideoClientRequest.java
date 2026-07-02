package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageGenerateVideoClientRequest {
    private String consumeCode;
    private String product;
    private String material;
    /**
     * 必须是外部可访问的url
     */
    private String imageUrl;
    private String prompt;
    /**
     * 卖点信息
     */
    private String sellingPoints;
}
