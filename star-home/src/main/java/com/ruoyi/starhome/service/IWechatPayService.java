package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.dto.WechatJsSdkSignatureResponse;
import com.ruoyi.starhome.domain.dto.WechatPayOrderResponse;
import com.ruoyi.starhome.domain.dto.WechatPayRechargeRequest;

/**
 * 微信支付服务
 */
public interface IWechatPayService {

    /**
     * 创建 JSAPI 充值订单
     */
    WechatPayOrderResponse createJsapiRechargeOrder(WechatPayRechargeRequest request);

    /**
     * 创建 Native 充值订单
     */
    WechatPayOrderResponse createNativeRechargeOrder(WechatPayRechargeRequest request);

    /**
     * 处理微信支付通知
     */
    String handleNotify(String requestBody, String timestamp, String nonce, String serial, String signature);

    /**
     * 根据订单号查询充值订单
     */
    FurnitureRechargeOrderDO getOrderByOrderNo(String orderNo);

    /**
     * 主动查询订单支付状态
     */
    FurnitureRechargeOrderDO queryPayStatus(String orderNo);

    /**
     * 生成微信 JSSDK 签名
     */
    WechatJsSdkSignatureResponse getJsSdkSignature(String url);
}
