package com.ruoyi.starhome.sms.service;

/**
 * 短信验证码服务
 *
 * @author starhome
 */
public interface ISmsCodeService {

    /**
     * 发送注册短信验证码
     *
     * @param phone 手机号
     */
    void sendRegisterCode(String phone);

    /**
     * 校验短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 校验通过返回 true
     */
    boolean verifyCode(String phone, String code);
}
