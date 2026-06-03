package com.ruoyi.starhome.domain.dto;

import com.ruoyi.starhome.enums.ConsumeConstants;
import lombok.Data;

import java.util.List;

@Data
public class ImageGenerateVideoRequest {
    /**
     * 可选：请求上下文无登录态（如定时任务）时用于透传用户ID。
     */
    private Long userId;
    private String apiNumber;
    private String product;
    private String material;
    private List<String> imageUrls;
    private String prompt;
    private Boolean generateDescription;
    private Long generationTaskId;
    private ConsumeConstants consumeConstants;
}
