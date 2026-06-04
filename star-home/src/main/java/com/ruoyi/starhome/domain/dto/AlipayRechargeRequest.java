package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付宝充值请求DTO
 */
@Data
@Schema(description = "支付宝充值请求")
public class AlipayRechargeRequest {

    @Schema(description = "用户ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "充值金额(元)", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "订单标题", example = "账户充值")
    private String subject;

    @Schema(description = "订单描述", example = "用户余额充值100元")
    private String body;
}
