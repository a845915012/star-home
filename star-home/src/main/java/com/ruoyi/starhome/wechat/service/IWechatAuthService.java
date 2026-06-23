package com.ruoyi.starhome.wechat.service;

import com.ruoyi.starhome.wechat.domain.dto.WechatCode2SessionResponse;

/**
 * 微信授权服务
 *
 * <p>负责调用微信服务端接口，通过 code 换取 openid / session_key 等信息。</p>
 *
 * @author starhome
 */
public interface IWechatAuthService {

    /**
     * 小程序登录：通过 wx.login() 获取的临时 code 换取 openid 和 session_key
     *
     * <p>调用 <a href="https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html">
     * code2Session</a> 接口。</p>
     *
     * @param code 前端 wx.login() 返回的临时凭证
     * @return code2Session 响应（包含 openid / session_key / unionid）
     */
    WechatCode2SessionResponse code2Session(String code);

    /**
     * 公众号网页授权：通过 OAuth2 code 换取 access_token 和 openid
     *
     * <p>调用 <a href="https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html">
     * 网页授权 access_token</a> 接口。</p>
     *
     * @param code 公众号网页授权回调返回的 code
     * @return 网页授权响应（包含 access_token / openid / unionid 等）
     */
    WechatCode2SessionResponse oauth2AccessToken(String code);
}
