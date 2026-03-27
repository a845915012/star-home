package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageGenerateVideoRequest {
    private String apiNumber;
    private List<String> imageUrls;
    private String prompt;
    private Boolean generateDescription;
}
