package com.ruoyi.starhome.wechat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.starhome.config.WechatPayConfig;
import com.ruoyi.starhome.wechat.domain.dto.WechatCode2SessionResponse;
import com.ruoyi.starhome.wechat.service.IWechatAuthService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 微信授权服务实现
 *
 * <p>支持小程序 code2Session 和公众号网页授权两种方式获取 openid。</p>
 *
 * @author starhome
 */
@Service
public class WechatAuthServiceImpl implements IWechatAuthService {

    private static final Logger log = LoggerFactory.getLogger(WechatAuthServiceImpl.class);

    /**
     * 小程序 code2Session 接口地址
     */
    private static final String CODE2SESSION_URL = "/sns/jscode2session";

    /**
     * 公众号网页授权 access_token 接口地址
     */
    private static final String OAUTH2_ACCESS_TOKEN_URL = "/sns/oauth2/access_token";

    @Autowired
    private WechatPayConfig wechatPayConfig;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WechatCode2SessionResponse code2Session(String code) {
        if (StringUtils.isEmpty(code)) {
            throw new ServiceException("code 不能为空");
        }
        ensureMiniProgramConfig();

        String url = wechatPayConfig.getMpApiBaseUrl()
                + CODE2SESSION_URL
                + "?appid=" + URLEncoder.encode(wechatPayConfig.getAppId(), StandardCharsets.UTF_8)
                + "&secret=" + URLEncoder.encode(wechatPayConfig.getAppSecret(), StandardCharsets.UTF_8)
                + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        JsonNode response = executeGet(url);
        return parseCode2SessionResponse(response);
    }

    @Override
    public WechatCode2SessionResponse oauth2AccessToken(String code) {
        if (StringUtils.isEmpty(code)) {
            throw new ServiceException("code 不能为空");
        }
        ensureOfficialAccountConfig();

        String url = wechatPayConfig.getMpApiBaseUrl()
                + OAUTH2_ACCESS_TOKEN_URL
                + "?appid=" + URLEncoder.encode(wechatPayConfig.getAppId(), StandardCharsets.UTF_8)
                + "&secret=" + URLEncoder.encode(wechatPayConfig.getAppSecret(), StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        JsonNode response = executeGet(url);
        return parseOAuth2Response(response);
    }

    // ---------- 内部方法 ----------

    private JsonNode executeGet(String url) {
        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                log.debug("微信授权接口响应: {}", responseBody);

                if (!response.isSuccessful()) {
                    throw new ServiceException("调用微信授权接口失败: HTTP " + response.code());
                }
                if (StringUtils.isEmpty(responseBody)) {
                    throw new ServiceException("微信授权接口返回空响应");
                }
                return objectMapper.readTree(responseBody);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("调用微信授权接口异常: " + e.getMessage());
        }
    }

    /**
     * 解析小程序 code2Session 响应
     */
    private WechatCode2SessionResponse parseCode2SessionResponse(JsonNode node) {
        WechatCode2SessionResponse result = new WechatCode2SessionResponse();

        int errcode = node.path("errcode").asInt(0);
        String errmsg = node.path("errmsg").asText("ok");
        result.setErrcode(errcode);
        result.setErrmsg(errmsg);

        if (errcode != 0) {
            log.error("微信 code2Session 失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new ServiceException("微信登录失败: " + errmsg);
        }

        result.setOpenid(node.path("openid").asText(null));
        result.setSessionKey(node.path("session_key").asText(null));
        result.setUnionid(node.path("unionid").asText(null));

        if (StringUtils.isEmpty(result.getOpenid())) {
            throw new ServiceException("获取微信 openid 失败");
        }

        return result;
    }

    /**
     * 解析公众号网页授权 OAuth2 access_token 响应
     */
    private WechatCode2SessionResponse parseOAuth2Response(JsonNode node) {
        WechatCode2SessionResponse result = new WechatCode2SessionResponse();

        int errcode = node.path("errcode").asInt(0);
        String errmsg = node.path("errmsg").asText("ok");
        result.setErrcode(errcode);
        result.setErrmsg(errmsg);

        if (errcode != 0) {
            log.error("微信网页授权失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new ServiceException("微信网页授权失败: " + errmsg);
        }

        // 公众号网页授权返回 access_token（不是 session_key），这里复用 sessionKey 字段存放
        String accessToken = node.path("access_token").asText(null);
        result.setSessionKey(accessToken);
        result.setOpenid(node.path("openid").asText(null));
        result.setUnionid(node.path("unionid").asText(null));

        if (StringUtils.isEmpty(result.getOpenid())) {
            throw new ServiceException("获取微信 openid 失败");
        }

        return result;
    }

    private void ensureMiniProgramConfig() {
        assertConfigNotBlank(wechatPayConfig.getAppId(), "wechatpay.app-id 未配置");
        assertConfigNotBlank(wechatPayConfig.getAppSecret(), "wechatpay.app-secret 未配置");
    }

    private void ensureOfficialAccountConfig() {
        assertConfigNotBlank(wechatPayConfig.getAppId(), "wechatpay.app-id 未配置");
        assertConfigNotBlank(wechatPayConfig.getAppSecret(), "wechatpay.app-secret 未配置");
    }

    private void assertConfigNotBlank(String value, String message) {
        if (StringUtils.isEmpty(value)) {
            throw new ServiceException(message);
        }
    }
}
