package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 视频生成头任务分页响应
 */
@Data
public class FurnitureVideoGenerationTaskPageResp {
    private long total;
    private List<FurnitureVideoGenerationTaskPageItemResp> list;
}
