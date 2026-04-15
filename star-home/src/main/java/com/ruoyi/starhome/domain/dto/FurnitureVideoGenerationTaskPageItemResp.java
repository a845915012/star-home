package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频生成头任务分页项
 */
@Data
public class FurnitureVideoGenerationTaskPageItemResp {
    private Long id;
    private Long userId;
    private String product;
    private String material;
    private String imageUrl;
    private Integer expectedTaskCount;
    private Integer currentTaskCount;
    private String status;
    private String localFinalVideoUrl;
    private String remoteFinalVideoUrl;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
