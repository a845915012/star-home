package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付宝充值请求DTO
 */
@Data
@Schema(description = "支付宝充值请求")
public class AlipayRechargeRequest {

    @Schema(description = "充值套餐ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long packageId;

    @Schema(description = "订单标题", example = "账户充值")
    private String subject;

    @Schema(description = "订单描述", example = "用户余额充值")
    private String body;
}
