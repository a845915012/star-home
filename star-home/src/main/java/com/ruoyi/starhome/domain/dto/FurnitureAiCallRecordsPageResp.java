package com.ruoyi.starhome.domain.dto;

import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FurnitureAiCallRecordsPageResp {
    private List<FurnitureAiCallRecordsSummary> summary;
    private List<FurnitureAiCallRecordsDO> list;

}
