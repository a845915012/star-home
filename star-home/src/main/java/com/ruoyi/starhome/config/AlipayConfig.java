package com.ruoyi.starhome.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝支付配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /**
     * 支付宝网关地址
     * 正式环境: https://openapi.alipay.com/gateway.do
     * 沙箱环境: https://openapi-sandbox.dl.alipaydev.com/gateway.do
     */
    private String serverUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 应用ID (APPID)
     */
    private String appId;

    /**
     * 商户私钥 (应用私钥)
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 字符编码
     */
    private String charset = "UTF-8";

    /**
     * 数据格式
     */
    private String format = "json";

    /**
     * 同步回调地址 (支付成功后跳转的页面)
     */
    private String returnUrl;

    /**
     * 异步通知地址 (支付宝服务器主动通知的地址)
     */
    private String notifyUrl;

    /**
     * 创建支付宝客户端
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                serverUrl,
                appId,
                privateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }
}
