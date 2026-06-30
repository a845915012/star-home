package com.ruoyi.starhome.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付与公众号配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechatpay")
public class WechatPayConfig {

    /**
     * 微信支付 API 地址
     */
    private String serverUrl = "https://api.mch.weixin.qq.com";

    /**
     * 微信公众号 API 地址
     */
    private String mpApiBaseUrl = "https://api.weixin.qq.com";

    /**
     * 公众号/JSAPI 应用ID
     */
    private String appId;

    /**
     * 公众号密钥
     */
    private String appSecret;

    /**
     * 微信支付商户号
     */
    private String mchId;

    /**
     * 商户证书序列号
     */
    private String mchSerialNo;

    /**
     * 商户私钥，支持 PEM 内容
     */
    private String privateKey;

    /**
     * 微信支付平台公钥，优先用于通知验签
     */
    private String platformPublicKey;

    /**
     * APIv3 密钥，用于解密支付通知
     */
    private String apiV3Key;

    /**
     * 异步通知地址
     */
    private String notifyUrl;

    /**
     * 模板消息ID - 视频生成成功通知
     * 需在微信公众号后台「广告与服务 → 模板消息」中申请
     */
    private String videoSuccessTemplateId;

    /**
     * 模板消息ID - 视频生成失败通知
     */
    private String videoFailTemplateId;

    /**
     * 模板消息ID - 充值成功通知（可选）
     */
    private String rechargeSuccessTemplateId;

    /**
     * 模板消息ID - 视频生成结果统一通知（可选）
     * 模板字段：character_string6(工单编号)、thing7(工单名称)、time3(结束时间)、const4(处理结果)
     */
    private String videoResultTemplateId;
}
