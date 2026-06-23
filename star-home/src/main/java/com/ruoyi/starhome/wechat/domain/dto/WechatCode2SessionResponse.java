package com.ruoyi.starhome.wechat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信 code2Session 响应结果
 *
 * <p>通过前端 wx.login() 获取的 code 调用微信服务端接口
 * <a href="https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html">code2Session</a>
 * 获取 openid 和 session_key。</p>
 *
 * <p>若用于公众号网页授权，则使用
 * <a href="https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html">网页授权</a>
 * 接口，该接口返回 openid + access_token + refresh_token 等信息。</p>
 *
 * @author starhome
 */
@Data
@Schema(description = "微信 code2Session 响应")
public class WechatCode2SessionResponse {

    @Schema(description = "用户唯一标识", example = "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o")
    private String openid;

    @Schema(description = "会话密钥", example = "HyVFkGl5F5OQWJZZaNzBBg==")
    private String sessionKey;

    @Schema(description = "用户在开放平台的唯一标识符（UnionID机制下返回）", example = "o6_bmasdasdsad6_2sgVt7hMZOPfL")
    private String unionid;

    @Schema(description = "错误码，0 表示成功", example = "0")
    private Integer errcode;

    @Schema(description = "错误信息", example = "ok")
    private String errmsg;

    /**
     * 调用是否成功
     */
    public boolean isSuccess() {
        return errcode == null || errcode == 0;
    }
}
