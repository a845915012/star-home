package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FurnitureUserBalanceOperateRequest {
    @Schema(description = "用户ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "金额", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
