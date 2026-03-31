package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 视频任务分页响应
 */
@Data
public class FurnitureVideoTaskPageResp {
    private long total;
    private List<FurnitureVideoTaskPageVO> list;
}
