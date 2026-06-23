package com.ruoyi.starhome.wechat.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.wechat.domain.dto.WechatAuthRequest;
import com.ruoyi.starhome.wechat.domain.dto.WechatCode2SessionResponse;
import com.ruoyi.starhome.wechat.service.IWechatAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信授权控制器
 *
 * <p>提供微信小程序登录和公众号网页授权接口，前端通过微信 SDK 获取临时 code 后，
 * 调用本接口换取 openid 和 session_key 等信息。</p>
 *
 * @author starhome
 */
@Tag(name = "微信授权")
@RestController
@RequestMapping("/starhome/wechat/auth")
public class WechatAuthController extends BaseController {

    @Autowired
    private IWechatAuthService wechatAuthService;

    /**
     * 小程序登录：通过 wx.login() 获取的 code 换取 openid
     *
     * <p>前端在小程序端调用 wx.login() 获取临时 code，传给后端换取 openid 和 session_key。
     * 该接口为匿名访问，不需要登录态。</p>
     *
     * @param request 包含 code 的请求体
     * @return openid / session_key / unionid
     */
    @Anonymous
    @Operation(summary = "小程序 code2Session", description = "小程序端通过 wx.login() 获取的临时 code 换取 openid 和 session_key")
    @PostMapping("/code2session")
    public AjaxResult code2Session(@Valid @RequestBody WechatAuthRequest request) {
        WechatCode2SessionResponse response = wechatAuthService.code2Session(request.getCode());
        return success(response);
    }

    /**
     * 公众号网页授权：通过 OAuth2 code 换取 openid
     *
     * <p>前端在公众号网页中引导用户授权后，微信会回调携带 code 参数，
     * 前端将 code 传给后端换取 openid 和 access_token。</p>
     *
     * @param request 包含 code 的请求体
     * @return openid / access_token / unionid
     */
    @Anonymous
    @Operation(summary = "公众号网页授权", description = "公众号网页授权回调后，通过 code 换取 access_token 和 openid")
    @PostMapping("/oauth2")
    public AjaxResult oauth2(@Valid @RequestBody WechatAuthRequest request) {
        WechatCode2SessionResponse response = wechatAuthService.oauth2AccessToken(request.getCode());
        return success(response);
    }
}
