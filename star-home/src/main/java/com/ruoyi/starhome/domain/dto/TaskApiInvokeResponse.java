package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TaskApiInvokeResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String apiNumber;
    private BigDecimal callCost;
    private String apiResult;
    private Integer remainingCalls;
    private Integer usedCalls;
}
