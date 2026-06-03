package com.ruoyi.starhome.domain.dto;

import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import lombok.Data;

import java.util.List;

@Data
public class FurnitureAiCallRecordsPageResp {
    private List<FurnitureAiCallRecordsDO> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;
}
