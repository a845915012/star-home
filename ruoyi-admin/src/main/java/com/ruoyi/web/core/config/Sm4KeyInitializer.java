package com.ruoyi.web.core.config;

import com.ruoyi.common.utils.sign.Sm4Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * SM4密钥初始化配置
 * <p>
 * 应用启动时从配置文件读取SM4密钥并注入到Sm4Utils工具类
 * </p>
 *
 * @author starhome
 */
@Component
public class Sm4KeyInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Sm4KeyInitializer.class);

    @Value("${sm4.key:StarHomeSM4Key!@#}")
    private String sm4Key;

    @Override
    public void run(ApplicationArguments args) {
        Sm4Utils.setKey(sm4Key);
        log.info("SM4密钥初始化完成");
    }
}
