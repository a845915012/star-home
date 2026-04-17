package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FurnitureAiCallRecordsOverview {
    private String timeRange;
    private Long totalCount;
    private BigDecimal totalToken;
    private BigDecimal totalAmount;
    private List<FurnitureAiCallRecordsSummary> summary;
}
