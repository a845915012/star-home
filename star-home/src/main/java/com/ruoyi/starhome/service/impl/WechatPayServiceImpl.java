package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.security.util.SecurityFrameworkUtils;
import com.ruoyi.starhome.config.WechatPayConfig;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.FurnitureRechargePackageDO;
import com.ruoyi.starhome.domain.dto.WechatJsSdkSignatureResponse;
import com.ruoyi.starhome.domain.dto.WechatPayOrderResponse;
import com.ruoyi.starhome.domain.dto.WechatPayRechargeRequest;
import com.ruoyi.starhome.enums.PayStatusConstants;
import com.ruoyi.starhome.enums.PayWayConstants;
import com.ruoyi.starhome.mapper.FurnitureRechargeOrderMapper;
import com.ruoyi.starhome.service.IFurnitureRechargePackageService;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import com.ruoyi.starhome.service.IWechatNotifyService;
import com.ruoyi.starhome.service.IWechatPayService;
import com.ruoyi.system.mapper.SysUserMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.starhome.util.WechatPayCryptoUtils;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 微信支付服务实现
 */
@Service
public class WechatPayServiceImpl implements IWechatPayService {

    private static final Logger log = LoggerFactory.getLogger(WechatPayServiceImpl.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String SUCCESS_NOTIFY_RESPONSE = "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    private static final String FAIL_NOTIFY_RESPONSE = "{\"code\":\"FAIL\",\"message\":\"失败\"}";
    private static final String CACHE_ACCESS_TOKEN_KEY = "wechatpay:access_token:";
    private static final String CACHE_JSAPI_TICKET_KEY = "wechatpay:jsapi_ticket:";
    private static final int VIP_ENABLED = 1;

    @Autowired
    private WechatPayConfig wechatPayConfig;

    @Autowired
    private FurnitureRechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private IFurnitureUserBalanceAccountService balanceAccountService;

    @Autowired
    private IFurnitureRechargePackageService furnitureRechargePackageService;

    @Autowired
    private IWechatNotifyService wechatNotifyService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RedisCache redisCache;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatPayOrderResponse createJsapiRechargeOrder(WechatPayRechargeRequest request) {
        ensureWechatPayConfig();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        SysUser sysUser = sysUserMapper.selectUserById(userId);
        if (StringUtils.isEmpty(sysUser.getWxOpenid())) {
            throw new ServiceException("JSAPI支付缺少openId");
        }


        RechargeInfo rechargeInfo = validateRequestAndBuildRechargeInfo(request);
        FurnitureRechargeOrderDO order = createOrder(userId, rechargeInfo, request);

        Map<String, Object> payload = buildUnifiedPayRequest(order);
        Map<String, String> payer = new HashMap<>();
        payer.put("openid", sysUser.getWxOpenid());
        payload.put("payer", payer);

        JsonNode response = executeWechatPayRequest("POST", "/v3/pay/transactions/jsapi", payload);
        String prepayId = requiredText(response, "prepay_id", "微信JSAPI下单失败");
        return buildJsapiResponse(order.getOrderNo(), prepayId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatPayOrderResponse createNativeRechargeOrder(WechatPayRechargeRequest request) {
        ensureWechatPayConfig();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        RechargeInfo rechargeInfo = validateRequestAndBuildRechargeInfo(request);
        FurnitureRechargeOrderDO order = createOrder(userId, rechargeInfo, request);

        JsonNode response = executeWechatPayRequest("POST", "/v3/pay/transactions/native", buildUnifiedPayRequest(order));
        String prepayId = optionalText(response, "prepay_id");
        String codeUrl = requiredText(response, "code_url", "微信Native下单失败");

        WechatPayOrderResponse result = new WechatPayOrderResponse();
        result.setOrderNo(order.getOrderNo());
        result.setAppId(wechatPayConfig.getAppId());
        result.setPrepayId(prepayId);
        result.setCodeUrl(codeUrl);
        result.setSignType("RSA");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatPayOrderResponse createH5RechargeOrder(WechatPayRechargeRequest request) {
        ensureWechatPayConfig();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        RechargeInfo rechargeInfo = validateRequestAndBuildRechargeInfo(request);
        FurnitureRechargeOrderDO order = createOrder(userId, rechargeInfo, request);

        Map<String, Object> payload = buildUnifiedPayRequest(order);
        Map<String, Object> sceneInfo = new HashMap<>();
        Map<String, Object> h5Info = new HashMap<>();
        h5Info.put("type", "Wap");
        sceneInfo.put("payer_client_ip", request.getClientIp());
        sceneInfo.put("h5_info", h5Info);
        payload.put("scene_info", sceneInfo);

        JsonNode response = executeWechatPayRequest("POST", "/v3/pay/transactions/h5", payload);
        String prepayId = optionalText(response, "prepay_id");
        String h5Url = requiredText(response, "h5_url", "微信H5下单失败");

        WechatPayOrderResponse result = new WechatPayOrderResponse();
        result.setOrderNo(order.getOrderNo());
        result.setAppId(wechatPayConfig.getAppId());
        result.setPrepayId(prepayId);
        result.setH5Url(h5Url);
        result.setSignType("RSA");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(String requestBody, String timestamp, String nonce, String serial, String signature) {
        log.info("收到微信支付异步通知, serial={}", serial);

        try {
            ensureNotifyConfig();
            String signMessage = timestamp + "\n" + nonce + "\n" + requestBody + "\n";
            boolean verified = WechatPayCryptoUtils.verifyWithRsaSha256(
                    signMessage,
                    signature,
                    wechatPayConfig.getPlatformPublicKey()
            );
            if (!verified) {
                log.error("微信支付通知验签失败");
                return FAIL_NOTIFY_RESPONSE;
            }

            JsonNode notifyJson = readJson(requestBody);
            JsonNode resource = notifyJson.path("resource");
            String plainText = WechatPayCryptoUtils.decryptAesGcm(
                    wechatPayConfig.getApiV3Key(),
                    optionalText(resource, "associated_data"),
                    requiredText(resource, "nonce", "微信支付通知缺少nonce"),
                    requiredText(resource, "ciphertext", "微信支付通知缺少ciphertext")
            );
            JsonNode transaction = readJson(plainText);

            String orderNo = requiredText(transaction, "out_trade_no", "微信支付通知缺少订单号");
            FurnitureRechargeOrderDO order = getOrderByOrderNo(orderNo);
            if (order == null) {
                log.error("微信充值订单不存在: {}", orderNo);
                return FAIL_NOTIFY_RESPONSE;
            }
            if (order.getPayStatus() == PayStatusConstants.SUCCESS) {
                return SUCCESS_NOTIFY_RESPONSE;
            }

            String tradeState = optionalText(transaction, "trade_state");
            if ("SUCCESS".equals(tradeState)) {
                assertPaidAmount(order, transaction.path("amount"));
                order.setPayStatus(PayStatusConstants.SUCCESS);
                order.setTransactionId(optionalText(transaction, "transaction_id"));
                order.setPayTime(parseWechatTime(optionalText(transaction, "success_time")));
                order.setNotifyTime(new Date());
                order.setNotifyContent(requestBody);
                order.setUpdateTime(new Date());
                int rows = rechargeOrderMapper.updatePaySuccessIfNotAlready(order);
                if (rows == 0) {
                    log.warn("微信支付通知重复处理, orderNo={}", orderNo);
                    return SUCCESS_NOTIFY_RESPONSE;
                }

                balanceAccountService.recharge(order.getUserId(), getProvideAmountOrPayAmount(order), PayWayConstants.WECHAT);
                updateUserVipStatusIfNeeded(order);

                // 推送充值成功模板消息
                try {
                    BigDecimal provideAmount = getProvideAmountOrPayAmount(order);
                    BigDecimal balance = getCoinBalance(order.getUserId());
                    wechatNotifyService.notifyRechargeSuccess(
                            order.getUserId(),
                            orderNo,
                            order.getAmount().toString(),
                            PayWayConstants.WECHAT,
                            provideAmount.toString(),
                            balance.toString()
                    );
                } catch (Exception e) {
                    log.error("发送充值成功模板消息失败 orderNo={}", orderNo, e);
                }

                log.info("微信充值订单支付成功, orderNo={}, transactionId={}", orderNo, order.getTransactionId());
            } else if ("CLOSED".equals(tradeState)) {
                order.setPayStatus(PayStatusConstants.CLOSED);
                order.setNotifyTime(new Date());
                order.setNotifyContent(requestBody);
                order.setUpdateTime(new Date());
                rechargeOrderMapper.updateById(order);
            } else if ("PAYERROR".equals(tradeState) || "REVOKED".equals(tradeState)) {
                order.setPayStatus(PayStatusConstants.FAILED);
                order.setNotifyTime(new Date());
                order.setNotifyContent(requestBody);
                order.setUpdateTime(new Date());
                rechargeOrderMapper.updateById(order);
            }
            return SUCCESS_NOTIFY_RESPONSE;
        } catch (Exception e) {
            log.error("处理微信支付通知异常", e);
            return FAIL_NOTIFY_RESPONSE;
        }
    }

    @Override
    public FurnitureRechargeOrderDO getOrderByOrderNo(String orderNo) {
        return rechargeOrderMapper.selectOne(
                new LambdaQueryWrapper<FurnitureRechargeOrderDO>()
                        .eq(FurnitureRechargeOrderDO::getOrderNo, orderNo)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FurnitureRechargeOrderDO queryPayStatus(String orderNo) {
        ensureWechatPayConfig();
        FurnitureRechargeOrderDO order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getPayStatus() == PayStatusConstants.SUCCESS ||
                order.getPayStatus() == PayStatusConstants.CLOSED ||
                order.getPayStatus() == PayStatusConstants.FAILED) {
            return order;
        }

        String encodedOrderNo = URLEncoder.encode(orderNo, StandardCharsets.UTF_8);
        String path = "/v3/pay/transactions/out-trade-no/" + encodedOrderNo
                + "?mchid=" + URLEncoder.encode(wechatPayConfig.getMchId(), StandardCharsets.UTF_8);
        JsonNode response = executeWechatPayRequest("GET", path, null);
        String tradeState = optionalText(response, "trade_state");

        if ("SUCCESS".equals(tradeState)) {
            assertPaidAmount(order, response.path("amount"));
            order.setPayStatus(PayStatusConstants.SUCCESS);
            order.setTransactionId(optionalText(response, "transaction_id"));
            order.setPayTime(parseWechatTime(optionalText(response, "success_time")));
            order.setUpdateTime(new Date());
            int rows = rechargeOrderMapper.updatePaySuccessIfNotAlready(order);
            if (rows == 0) {
                return order;
            }

            balanceAccountService.recharge(order.getUserId(), getProvideAmountOrPayAmount(order), PayWayConstants.WECHAT);
            updateUserVipStatusIfNeeded(order);

            // 推送充值成功模板消息
            try {
                BigDecimal provideAmount = getProvideAmountOrPayAmount(order);
                BigDecimal balance = getCoinBalance(order.getUserId());
                wechatNotifyService.notifyRechargeSuccess(
                        order.getUserId(),
                        orderNo,
                        order.getAmount().toString(),
                        PayWayConstants.WECHAT,
                        provideAmount.toString(),
                        balance.toString()
                );
            } catch (Exception e) {
                log.error("发送充值成功模板消息失败 orderNo={}", orderNo, e);
            }
        } else if ("CLOSED".equals(tradeState)) {
            order.setPayStatus(PayStatusConstants.CLOSED);
            order.setUpdateTime(new Date());
            rechargeOrderMapper.updateById(order);
        } else if ("PAYERROR".equals(tradeState) || "REVOKED".equals(tradeState)) {
            order.setPayStatus(PayStatusConstants.FAILED);
            order.setUpdateTime(new Date());
            rechargeOrderMapper.updateById(order);
        }
        return order;
    }

    @Override
    public WechatJsSdkSignatureResponse getJsSdkSignature(String url) {
        ensureJsSdkConfig();
        String normalizedUrl = normalizeUrl(url);
        String accessToken = getAccessToken();
        String jsapiTicket = getJsapiTicket(accessToken);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String rawString = "jsapi_ticket=" + jsapiTicket
                + "&noncestr=" + nonceStr
                + "&timestamp=" + timestamp
                + "&url=" + normalizedUrl;

        WechatJsSdkSignatureResponse response = new WechatJsSdkSignatureResponse();
        response.setAppId(wechatPayConfig.getAppId());
        response.setUrl(normalizedUrl);
        response.setTimestamp(timestamp);
        response.setNonceStr(nonceStr);
        response.setSignature(WechatPayCryptoUtils.sha1Hex(rawString));
        return response;
    }

    private RechargeInfo validateRequestAndBuildRechargeInfo(WechatPayRechargeRequest request) {
        boolean hasPackageId = request.getPackageId() != null;
        boolean hasAmount = request.getAmount() != null;

        if (hasPackageId && hasAmount) {
            throw new ServiceException("充值套餐ID和自定义金额不能同时传");
        }
        if (!hasPackageId && !hasAmount) {
            throw new ServiceException("充值套餐ID和自定义金额不能同时为空");
        }

        if (hasPackageId) {
            FurnitureRechargePackageDO rechargePackage = furnitureRechargePackageService.selectEnabledById(request.getPackageId());
            if (rechargePackage == null) {
                throw new ServiceException("充值套餐不存在或未启用");
            }
            if (rechargePackage.getCostAmount() == null || rechargePackage.getCostAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("充值套餐支付金额异常");
            }
            if (rechargePackage.getCostAmount().compareTo(new BigDecimal("50000")) > 0) {
                throw new ServiceException("单笔充值金额不能超过50000元");
            }
            if (rechargePackage.getProvideAmount() == null || rechargePackage.getProvideAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("充值套餐到账金额异常");
            }
            return new RechargeInfo(rechargePackage.getId(), rechargePackage.getCostAmount(), rechargePackage.getProvideAmount());
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("充值金额必须大于0");
        }
        if (request.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            throw new ServiceException("单笔充值金额不能超过50000元");
        }
        return new RechargeInfo(null, request.getAmount(), request.getAmount());
    }

    private FurnitureRechargeOrderDO createOrder(Long userId, RechargeInfo rechargeInfo, WechatPayRechargeRequest request) {
        FurnitureRechargeOrderDO order = new FurnitureRechargeOrderDO();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPackageId(rechargeInfo.packageId);
        order.setAmount(rechargeInfo.payAmount);
        order.setProvideAmount(rechargeInfo.provideAmount);
        order.setPayStatus(PayStatusConstants.PENDING);
        order.setPayWay(PayWayConstants.WECHAT);
        order.setSubject(StringUtils.isNotEmpty(request.getSubject()) ? request.getSubject() : "账户充值");
        order.setBody(StringUtils.isNotEmpty(request.getBody()) ? request.getBody()
                : "用户余额充值，支付" + order.getAmount() + "元，到账" + order.getProvideAmount() + "星币");
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        rechargeOrderMapper.insert(order);
        return order;
    }

    private Map<String, Object> buildUnifiedPayRequest(FurnitureRechargeOrderDO order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("appid", wechatPayConfig.getAppId());
        payload.put("mchid", wechatPayConfig.getMchId());
        payload.put("description", order.getSubject());
        payload.put("out_trade_no", order.getOrderNo());
        payload.put("notify_url", wechatPayConfig.getNotifyUrl());

        Map<String, Object> amount = new HashMap<>();
        amount.put("total", amountToFen(order.getAmount()));
        amount.put("currency", "CNY");
        payload.put("amount", amount);
        return payload;
    }

    private WechatPayOrderResponse buildJsapiResponse(String orderNo, String prepayId) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String packageValue = "prepay_id=" + prepayId;
        String message = wechatPayConfig.getAppId() + "\n"
                + timestamp + "\n"
                + nonceStr + "\n"
                + packageValue + "\n";

        WechatPayOrderResponse response = new WechatPayOrderResponse();
        response.setOrderNo(orderNo);
        response.setAppId(wechatPayConfig.getAppId());
        response.setPrepayId(prepayId);
        response.setTimeStamp(timestamp);
        response.setNonceStr(nonceStr);
        response.setPackageValue(packageValue);
        response.setSignType("RSA");
        response.setPaySign(WechatPayCryptoUtils.signWithRsaSha256(message, wechatPayConfig.getPrivateKey()));
        return response;
    }

    private JsonNode executeWechatPayRequest(String method, String canonicalPath, Object bodyObject) {
        try {
            String body = bodyObject == null ? "" : objectMapper.writeValueAsString(bodyObject);
            String url = wechatPayConfig.getServerUrl() + canonicalPath;
            RequestBody requestBody = "GET".equalsIgnoreCase(method) ? null : RequestBody.create(body, JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", buildAuthorizationHeader(method, canonicalPath, body))
                    .addHeader("Content-Type", "application/json")
                    .method(method, requestBody)
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    throw new ServiceException("调用微信支付接口失败: " + parseWechatError(responseBody));
                }
                return StringUtils.isEmpty(responseBody) ? objectMapper.createObjectNode() : readJson(responseBody);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("调用微信支付接口异常: " + e.getMessage());
        }
    }

    private String buildAuthorizationHeader(String method, String canonicalPath, String body) {
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String message = method + "\n"
                + canonicalPath + "\n"
                + timestamp + "\n"
                + nonceStr + "\n"
                + body + "\n";
        String signature = WechatPayCryptoUtils.signWithRsaSha256(message, wechatPayConfig.getPrivateKey());

        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + wechatPayConfig.getMchId() + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + wechatPayConfig.getMchSerialNo() + "\","
                + "signature=\"" + signature + "\"";
    }

    private String getAccessToken() {
        String cacheKey = CACHE_ACCESS_TOKEN_KEY + wechatPayConfig.getAppId();
        String cached = redisCache.getCacheObject(cacheKey);
        if (StringUtils.isNotEmpty(cached)) {
            return cached;
        }

        String url = wechatPayConfig.getMpApiBaseUrl()
                + "/cgi-bin/token?grant_type=client_credential&appid="
                + URLEncoder.encode(wechatPayConfig.getAppId(), StandardCharsets.UTF_8)
                + "&secret=" + URLEncoder.encode(wechatPayConfig.getAppSecret(), StandardCharsets.UTF_8);
        JsonNode response = executeSimpleGet(url);
        validateWechatMpResponse(response, "获取微信access_token失败");
        String accessToken = requiredText(response, "access_token", "获取微信access_token失败");
        long expiresIn = response.path("expires_in").asLong(7200);
        int ttlSeconds = (int) Math.max(60L, expiresIn - 200L);
        redisCache.setCacheObject(cacheKey, accessToken, ttlSeconds, TimeUnit.SECONDS);
        return accessToken;
    }

    private String getJsapiTicket(String accessToken) {
        String cacheKey = CACHE_JSAPI_TICKET_KEY + wechatPayConfig.getAppId();
        String cached = redisCache.getCacheObject(cacheKey);
        if (StringUtils.isNotEmpty(cached)) {
            return cached;
        }

        String url = wechatPayConfig.getMpApiBaseUrl()
                + "/cgi-bin/ticket/getticket?access_token="
                + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&type=jsapi";
        JsonNode response = executeSimpleGet(url);
        validateWechatMpResponse(response, "获取微信jsapi_ticket失败");
        String ticket = requiredText(response, "ticket", "获取微信jsapi_ticket失败");
        long expiresIn = response.path("expires_in").asLong(7200);
        int ttlSeconds = (int) Math.max(60L, expiresIn - 200L);
        redisCache.setCacheObject(cacheKey, ticket, ttlSeconds, TimeUnit.SECONDS);
        return ticket;
    }

    private JsonNode executeSimpleGet(String url) {
        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    throw new ServiceException("调用微信接口失败: " + responseBody);
                }
                return readJson(responseBody);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("调用微信接口异常: " + e.getMessage());
        }
    }

    private void validateWechatMpResponse(JsonNode response, String message) {
        int errCode = response.path("errcode").asInt(0);
        if (errCode != 0) {
            throw new ServiceException(message + ": " + response.path("errmsg").asText("未知错误"));
        }
    }

    private void assertPaidAmount(FurnitureRechargeOrderDO order, JsonNode amountNode) {
        long expectFen = amountToFen(order.getAmount());
        long actualFen = amountNode.path("payer_total").asLong(amountNode.path("total").asLong(-1));
        if (actualFen < 0 || actualFen != expectFen) {
            throw new ServiceException("支付金额校验失败");
        }
    }

    private long amountToFen(BigDecimal amount) {
        try {
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new ServiceException("金额格式不合法");
        }
    }

    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new ServiceException("解析微信返回数据失败: " + e.getMessage());
        }
    }

    private String parseWechatError(String responseBody) {
        if (StringUtils.isEmpty(responseBody)) {
            return "空响应";
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String code = optionalText(jsonNode, "code");
            String message = optionalText(jsonNode, "message");
            if (StringUtils.isNotEmpty(code) || StringUtils.isNotEmpty(message)) {
                return code + " " + message;
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }

    private String normalizeUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            throw new ServiceException("url不能为空");
        }
        String normalized = url.trim();
        int index = normalized.indexOf('#');
        if (index >= 0) {
            normalized = normalized.substring(0, index);
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            throw new ServiceException("url格式不正确");
        }
        return normalized;
    }

    private void ensureWechatPayConfig() {
        assertConfigNotBlank(wechatPayConfig.getAppId(), "wechatpay.app-id 未配置");
        assertConfigNotBlank(wechatPayConfig.getMchId(), "wechatpay.mch-id 未配置");
        assertConfigNotBlank(wechatPayConfig.getMchSerialNo(), "wechatpay.mch-serial-no 未配置");
        assertConfigNotBlank(wechatPayConfig.getPrivateKey(), "wechatpay.private-key 未配置");
        assertConfigNotBlank(wechatPayConfig.getNotifyUrl(), "wechatpay.notify-url 未配置");
    }

    private void ensureNotifyConfig() {
        ensureWechatPayConfig();
        assertConfigNotBlank(wechatPayConfig.getPlatformPublicKey(), "wechatpay.platform-public-key 未配置");
        assertConfigNotBlank(wechatPayConfig.getApiV3Key(), "wechatpay.api-v3-key 未配置");
    }

    private void ensureJsSdkConfig() {
        assertConfigNotBlank(wechatPayConfig.getAppId(), "wechatpay.app-id 未配置");
        assertConfigNotBlank(wechatPayConfig.getAppSecret(), "wechatpay.app-secret 未配置");
    }

    private void assertConfigNotBlank(String value, String message) {
        if (StringUtils.isEmpty(value)) {
            throw new ServiceException(message);
        }
    }

    private String requiredText(JsonNode node, String fieldName, String errorMessage) {
        String value = optionalText(node, fieldName);
        if (StringUtils.isEmpty(value)) {
            throw new ServiceException(errorMessage);
        }
        return value;
    }

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        String value = field.asText();
        return StringUtils.isEmpty(value) ? null : value;
    }

    private Date parseWechatTime(String time) {
        if (StringUtils.isEmpty(time)) {
            return new Date();
        }
        try {
            return Date.from(OffsetDateTime.parse(time).toInstant());
        } catch (Exception e) {
            return new Date();
        }
    }

    private BigDecimal getProvideAmountOrPayAmount(FurnitureRechargeOrderDO order) {
        if (order.getProvideAmount() != null) {
            return order.getProvideAmount();
        }
        return order.getAmount();
    }

    /**
     * 获取用户星币余额
     */
    private BigDecimal getCoinBalance(Long userId) {
        try {
            var account = balanceAccountService.selectFurnitureUserBalanceAccountByUserId(userId);
            return account != null && account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("获取用户余额失败 userId={}", userId, e);
            return BigDecimal.ZERO;
        }
    }

    private void updateUserVipStatusIfNeeded(FurnitureRechargeOrderDO order) {
        if (order.getPackageId() == null) {
            return;
        }
        FurnitureRechargePackageDO rechargePackage = furnitureRechargePackageService.selectFurnitureRechargePackageById(order.getPackageId());
        if (rechargePackage == null || !Integer.valueOf(VIP_ENABLED).equals(rechargePackage.getIsVip())) {
            return;
        }

        Integer vipDay = rechargePackage.getVipDay();
        SysUser dbUser = sysUserMapper.selectUserById(order.getUserId());
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime vipBeginTime = dbUser != null ? dbUser.getVipBeginTime() : null;
        LocalDateTime vipExpireTime = dbUser != null ? dbUser.getVipExpireTime() : null;
        Integer isVip = dbUser != null ? dbUser.getIsVip() : null;

        if (isVip == null || isVip == 0 || vipBeginTime == null) {
            vipBeginTime = now;
        }

        LocalDateTime baseExpireTime = vipExpireTime != null ? vipExpireTime : now;
        if (baseExpireTime.isBefore(now)) {
            baseExpireTime = now;
        }
        if (vipDay != null && vipDay > 0) {
            vipExpireTime = baseExpireTime.plusDays(vipDay);
        } else {
            vipExpireTime = baseExpireTime;
        }

        SysUser user = new SysUser();
        user.setUserId(order.getUserId());
        user.setIsVip(VIP_ENABLED);
        user.setVipBeginTime(vipBeginTime);
        user.setVipExpireTime(vipExpireTime);
        // 如果充值包设置了会员等级，则赋值到用户表
        if (rechargePackage.getVipLevel() != null) {
            user.setVipLevel(rechargePackage.getVipLevel());
        }
        sysUserMapper.updateUser(user);
    }

    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RC" + timestamp + uuid;
    }

    private record RechargeInfo(Long packageId, BigDecimal payAmount, BigDecimal provideAmount) {
    }
}
