package com.ruoyi.starhome.domain.dto;

/**
 * 忘记密码请求体
 *
 * @author starhome
 */
public class ForgetPasswordBody {

    /** 手机号 */
    private String phone;

    /** 新密码 */
    private String password;

    /** 短信验证码 */
    private String smsCode;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }
}
