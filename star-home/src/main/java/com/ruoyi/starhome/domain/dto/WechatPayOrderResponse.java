package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信支付下单响应
 */
@Data
@Schema(description = "微信支付下单响应")
public class WechatPayOrderResponse {

    @Schema(description = "充值订单号", example = "RC20260618112233A1B2C3D4")
    private String orderNo;

    @Schema(description = "微信应用ID", example = "wx1234567890abcdef")
    private String appId;

    @Schema(description = "预支付交易会话标识", example = "wx201410272009395522657a690389285100")
    private String prepayId;

    @Schema(description = "JSSDK/JSAPI 调起参数中的时间戳", example = "1718697600")
    private String timeStamp;

    @Schema(description = "JSSDK/JSAPI 调起参数中的随机串", example = "5K8264ILTKCH16CQ2502SI8ZNMTM67VS")
    private String nonceStr;

    @Schema(description = "JSAPI 调起参数中的 package", example = "prepay_id=wx201410272009395522657a690389285100")
    private String packageValue;

    @Schema(description = "签名类型", example = "RSA")
    private String signType;

    @Schema(description = "JSAPI 调起签名", example = "uOVRnA4q....")
    private String paySign;

    @Schema(description = "Native 支付二维码链接", example = "weixin://wxpay/bizpayurl?pr=abc123")
    private String codeUrl;

    @Schema(description = "H5 支付跳转链接", example = "https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=wx123456&package=abc")
    private String h5Url;
}
