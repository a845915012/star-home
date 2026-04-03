package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageGenerateVideoClientRequest {
    private String apiNumber;
    private String product;
    private String material;
    private List<String> imageUrls;
    private String prompt;
    private Boolean generateDescription;
}
