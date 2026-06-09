package com.ruoyi.starhome.sms.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.starhome.sms.config.AliyunSmsConfig;
import com.ruoyi.starhome.sms.service.ISmsCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 短信验证码服务实现
 *
 * 通过阿里云短信服务(HTTPS)真实发送验证码,验证码存入 Redis,
 * 并对同一手机号做发送频率限制(60秒)。
 *
 * @author starhome
 */
@Service
public class SmsCodeServiceImpl implements ISmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeServiceImpl.class);

    /** 手机号正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 验证码有效期(分钟) */
    private static final long CODE_EXPIRE_MINUTES = 5L;

    /** 发送频率限制(秒) */
    private static final long SEND_LIMIT_SECONDS = 60L;

    @Autowired
    private Client aliyunSmsClient;

    @Autowired
    private AliyunSmsConfig smsConfig;

    @Autowired
    private RedisCache redisCache;

    @Override
    public void sendRegisterCode(String phone) {
        if (StringUtils.isEmpty(phone) || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new ServiceException("手机号格式不正确");
        }

        // 频率限制：60秒内不可重复发送
        String limitKey = CacheConstants.SMS_SEND_LIMIT_KEY + phone;
        if (Boolean.TRUE.equals(redisCache.hasKey(limitKey))) {
            throw new ServiceException("验证码发送过于频繁,请稍后再试");
        }

        // 生成6位数字验证码
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // 调用阿里云短信发送(HTTPS)
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(smsConfig.getSignName())
                .setTemplateCode(smsConfig.getTemplateCode())
                .setTemplateParam("{\"code\":\"" + code + "\"}");
        try {
            SendSmsResponse response = aliyunSmsClient.sendSms(request);
            SendSmsResponseBody body = response.getBody();
            if (body == null || !"OK".equalsIgnoreCase(body.getCode())) {
                String errMsg = body == null ? "未知错误" : body.getMessage();
                log.error("[v0] 短信发送失败, phone={}, code={}, msg={}",
                        phone, body == null ? null : body.getCode(), errMsg);
                throw new ServiceException("短信发送失败:" + errMsg);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[v0] 调用阿里云短信服务异常, phone=" + phone, e);
            throw new ServiceException("短信发送异常,请稍后再试");
        }

        // 验证码存入 Redis,设置有效期
        redisCache.setCacheObject(CacheConstants.SMS_CODE_KEY + phone, code,
                (int) CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        // 设置发送频率限制标记
        redisCache.setCacheObject(limitKey, "1", (int) SEND_LIMIT_SECONDS, TimeUnit.SECONDS);
        log.info("[v0] 短信验证码已发送, phone={}", phone);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)) {
            return false;
        }
        String cacheKey = CacheConstants.SMS_CODE_KEY + phone;
        String cachedCode = redisCache.getCacheObject(cacheKey);
        if (StringUtils.isEmpty(cachedCode)) {
            return false;
        }
        boolean matched = code.equals(cachedCode);
        if (matched) {
            // 校验通过后立即删除,防止重复使用
            redisCache.deleteObject(cacheKey);
        }
        return matched;
    }
}
