package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class FurnitureAiCallRecordsSummary {
    private String modeName;
    private Integer count;
    private BigDecimal totalToken;
    private BigDecimal totalAmount;
}
