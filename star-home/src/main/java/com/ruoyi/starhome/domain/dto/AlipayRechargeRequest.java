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

    @Schema(description = "充值套餐ID，套餐充值时必传，和amount二选一", example = "1")
    private Long packageId;

    @Schema(description = "自定义充值金额，1:1充值时必传，和packageId二选一", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "订单标题", example = "账户充值")
    private String subject;

    @Schema(description = "订单描述", example = "用户余额充值")
    private String body;
}
