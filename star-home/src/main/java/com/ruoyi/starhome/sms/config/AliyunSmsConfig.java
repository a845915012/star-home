package com.ruoyi.starhome.sms.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信配置
 *
 * 敏感信息(access-key-id / access-key-secret)建议通过环境变量注入,
 * 在 application.yml 中以 ${ALIYUN_SMS_ACCESS_KEY_ID} 形式引用。
 *
 * @author starhome
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.sms")
public class AliyunSmsConfig {

    /**
     * 访问密钥 ID
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret
     */
    private String accessKeySecret;

    /**
     * 短信服务接入点 (使用 HTTPS)
     */
    private String endpoint = "dysmsapi.aliyuncs.com";

    /**
     * 短信签名
     */
    private String signName;

    /**
     * 验证码短信模板 CODE
     */
    private String templateCode;

    /**
     * 创建阿里云短信客户端 (走 HTTPS)
     */
    @Bean
    public Client aliyunSmsClient() throws Exception {
        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)) {
            throw new ServiceException("阿里云短信AccessKey未配置");
        }

        com.aliyun.credentials.models.Config credentialConfig = new com.aliyun.credentials.models.Config()
                .setType("access_key")
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        com.aliyun.credentials.Client credentialClient = new com.aliyun.credentials.Client(credentialConfig);

        Config config = new Config()
                .setCredential(credentialClient);
        // 显式指定 HTTPS 协议与接入点
        config.endpoint = endpoint;
        config.protocol = "HTTPS";
        return new Client(config);
    }
}
