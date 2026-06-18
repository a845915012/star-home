package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 微信充值请求
 */
@Data
@Schema(description = "微信充值请求")
public class WechatPayRechargeRequest {

    @Schema(description = "充值套餐ID，和 amount 二选一", example = "1")
    private Long packageId;

    @Schema(description = "自定义充值金额，和 packageId 二选一", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "订单标题", example = "账户充值")
    private String subject;

    @Schema(description = "订单描述", example = "余额充值")
    private String body;

    @Schema(description = "用户微信 openId，仅 JSAPI 支付必填", example = "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o")
    private String openId;
}
