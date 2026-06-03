package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付宝充值响应DTO
 */
@Data
@Schema(description = "支付宝充值响应")
public class AlipayRechargeResponse {

    @Schema(description = "充值订单号")
    private String orderNo;

    @Schema(description = "支付表单HTML(用于跳转支付宝)")
    private String payForm;

    @Schema(description = "支付链接(H5支付时返回)")
    private String payUrl;
}
