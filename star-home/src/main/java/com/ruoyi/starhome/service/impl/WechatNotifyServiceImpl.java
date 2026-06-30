package com.ruoyi.starhome.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.starhome.config.WechatPayConfig;
import com.ruoyi.starhome.domain.FurnitureWxNotifyRecordDO;
import com.ruoyi.starhome.enums.WxNotifyTypeConstants;
import com.ruoyi.starhome.mapper.FurnitureWxNotifyRecordMapper;
import com.ruoyi.starhome.service.IWechatNotifyService;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信模板消息通知服务实现
 * <p>
 * 通过微信公众号模板消息 API 向用户发送通知。
 * 模板消息需要在微信公众号后台申请，字段格式需与申请的模板一致。
 * <p>
 * 标准模板字段说明（需在公众号后台申请模板后确认）：
 * - first: 开头提示文字
 * - keyword1 ~ keyword5: 模板关键字
 * - remark: 结尾备注
 */
@Service
public class WechatNotifyServiceImpl implements IWechatNotifyService {

    private static final Logger log = LoggerFactory.getLogger(WechatNotifyServiceImpl.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String CACHE_ACCESS_TOKEN_KEY = "wechatpay:access_token:";

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WechatPayConfig wechatPayConfig;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private FurnitureWxNotifyRecordMapper notifyRecordMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    public void notifyVideoSuccess(Long userId, Long generationTaskId, String videoUrl) {
        String templateId = wechatPayConfig.getVideoSuccessTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            log.info("视频生成成功模板消息ID未配置，跳过通知 userId={}", userId);
            return;
        }
        sendTemplateMessage(userId, templateId, WxNotifyTypeConstants.VIDEO_SUCCESS,
                buildVideoSuccessData(generationTaskId, videoUrl),
                String.valueOf(generationTaskId));
    }

    @Override
    public void notifyVideoFailed(Long userId, Long generationTaskId, String reason) {
        String templateId = wechatPayConfig.getVideoFailTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            log.info("视频生成失败模板消息ID未配置，跳过通知 userId={}", userId);
            return;
        }
        sendTemplateMessage(userId, templateId, WxNotifyTypeConstants.VIDEO_FAIL,
                buildVideoFailData(generationTaskId, reason),
                String.valueOf(generationTaskId));
    }

    @Override
    public void notifyRechargeSuccess(Long userId, String orderNo, String amount,
                                       String payWay, String coinAmount, String coinBalance) {
        String templateId = wechatPayConfig.getRechargeSuccessTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            log.info("充值成功模板消息ID未配置，跳过通知 userId={}", userId);
            return;
        }
        sendTemplateMessage(userId, templateId, WxNotifyTypeConstants.RECHARGE_SUCCESS,
                buildRechargeSuccessData(orderNo, amount, payWay, coinAmount, coinBalance),
                orderNo);
    }

    @Override
    public void notifyVideoResult(Long userId, Long generationTaskId, String taskName,
                                   String finishTime, String result) {
        String templateId = wechatPayConfig.getVideoResultTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            log.info("视频结果通知模板消息ID未配置，跳过通知 userId={}", userId);
            return;
        }
        sendTemplateMessage(userId, templateId, WxNotifyTypeConstants.VIDEO_RESULT,
                buildVideoResultData(generationTaskId, taskName, finishTime, result),
                String.valueOf(generationTaskId));
    }

    /**
     * 发送模板消息核心方法
     */
    private void sendTemplateMessage(Long userId, String templateId,
                                      String notifyType, Map<String, Object> data, String bizId) {
        // 1. 查询用户 openid
        String openid = getUserOpenid(userId);
        if (StringUtils.isEmpty(openid)) {
            log.info("用户未绑定微信openid，跳过模板消息通知 userId={}", userId);
            return;
        }

        // 2. 创建通知记录（状态：待发送）
        FurnitureWxNotifyRecordDO record = new FurnitureWxNotifyRecordDO();
        record.setUserId(userId);
        record.setOpenid(openid);
        record.setNotifyType(notifyType);
        record.setTemplateId(templateId);
        record.setSendStatus(0);
        record.setBizId(bizId);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        try {
            record.setContent(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            log.error("序列化模板消息数据失败", e);
            record.setContent(data.toString());
        }
        notifyRecordMapper.insert(record);

        // 3. 发送模板消息
        try {
            String accessToken = getAccessToken();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("touser", openid);
            payload.put("template_id", templateId);
            payload.put("data", data);

            String body = objectMapper.writeValueAsString(payload);
            String url = wechatPayConfig.getMpApiBaseUrl()
                    + "/cgi-bin/message/template/send?access_token="
                    + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body, JSON))
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                JsonNode result = objectMapper.readTree(responseBody);
                int errCode = result.path("errcode").asInt(-1);

                if (errCode == 0) {
                    record.setSendStatus(1);
                    record.setSendTime(LocalDateTime.now());
                    log.info("微信模板消息发送成功 userId={}, notifyType={}, bizId={}", userId, notifyType, bizId);
                } else {
                    String errMsg = result.path("errmsg").asText("未知错误");
                    record.setSendStatus(2);
                    record.setErrorMsg("errcode=" + errCode + ", errmsg=" + errMsg);
                    log.error("微信模板消息发送失败 userId={}, errcode={}, errmsg={}", userId, errCode, errMsg);
                }
            }
        } catch (Exception e) {
            record.setSendStatus(2);
            record.setErrorMsg("发送异常: " + e.getMessage());
            log.error("微信模板消息发送异常 userId={}, notifyType={}", userId, notifyType, e);
        }

        record.setUpdateTime(LocalDateTime.now());
        notifyRecordMapper.updateById(record);
    }

    /**
     * 获取用户微信 openid
     */
    private String getUserOpenid(Long userId) {
        try {
            SysUser user = sysUserMapper.selectUserById(userId);
            if (user != null && StringUtils.isNotEmpty(user.getWxOpenid())) {
                return user.getWxOpenid();
            }
        } catch (Exception e) {
            log.error("查询用户openid失败 userId={}", userId, e);
        }
        return null;
    }

    /**
     * 获取微信公众号 access_token（复用 WechatPayServiceImpl 中的缓存 key）
     */
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

        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                String body = response.body() == null ? "" : response.body().string();
                JsonNode result = objectMapper.readTree(body);
                int errCode = result.path("errcode").asInt(0);
                if (errCode != 0) {
                    throw new ServiceException("获取微信access_token失败: " + result.path("errmsg").asText("未知错误"));
                }
                String accessToken = result.path("access_token").asText();
                long expiresIn = result.path("expires_in").asLong(7200);
                int ttlSeconds = (int) Math.max(60L, expiresIn - 200L);
                redisCache.setCacheObject(cacheKey, accessToken, ttlSeconds, TimeUnit.SECONDS);
                return accessToken;
            }
        } catch (Exception e) {
            throw new ServiceException("获取微信access_token异常: " + e.getMessage());
        }
    }

    /**
     * 构建视频生成成功通知数据
     * <p>
     * 注意：data 中的 key（如 first、keyword1、remark）需与公众号后台申请的模板字段完全一致。
     * 如果申请的模板字段名不同，需要对应调整。
     */
    private Map<String, Object> buildVideoSuccessData(Long generationTaskId, String videoUrl) {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, String> first = new HashMap<>();
        first.put("value", "您的视频已生成成功！");
        first.put("color", "#173177");
        data.put("first", first);

        Map<String, String> keyword1 = new HashMap<>();
        keyword1.put("value", String.valueOf(generationTaskId));
        keyword1.put("color", "#173177");
        data.put("keyword1", keyword1);

        Map<String, String> keyword2 = new HashMap<>();
        keyword2.put("value", "图片生成视频");
        keyword2.put("color", "#173177");
        data.put("keyword2", keyword2);

        Map<String, String> keyword3 = new HashMap<>();
        keyword3.put("value", "生成成功");
        keyword3.put("color", "#07C160");
        data.put("keyword3", keyword3);

        Map<String, String> remark = new HashMap<>();
        remark.put("value", "点击查看视频详情");
        remark.put("color", "#888888");
        data.put("remark", remark);

        return data;
    }

    /**
     * 构建视频生成失败通知数据
     */
    private Map<String, Object> buildVideoFailData(Long generationTaskId, String reason) {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, String> first = new HashMap<>();
        first.put("value", "很遗憾，您的视频生成失败");
        first.put("color", "#173177");
        data.put("first", first);

        Map<String, String> keyword1 = new HashMap<>();
        keyword1.put("value", String.valueOf(generationTaskId));
        keyword1.put("color", "#173177");
        data.put("keyword1", keyword1);

        Map<String, String> keyword2 = new HashMap<>();
        keyword2.put("value", "图片生成视频");
        keyword2.put("color", "#173177");
        data.put("keyword2", keyword2);

        Map<String, String> keyword3 = new HashMap<>();
        keyword3.put("value", "生成失败");
        keyword3.put("color", "#FF0000");
        data.put("keyword3", keyword3);

        Map<String, String> remark = new HashMap<>();
        remark.put("value", StringUtils.isNotEmpty(reason) ? "失败原因：" + reason : "请稍后重试或联系客服");
        remark.put("color", "#888888");
        data.put("remark", remark);

        return data;
    }

    /**
     * 构建充值成功通知数据
     * <p>
     * 模板字段：
     * - thing4:  充值分类（支付方式 + 到账星币）
     * - amount5: 充值金额
     * - amount6: 账户余额
     */
    private Map<String, Object> buildRechargeSuccessData(String orderNo, String amount,
                                                          String payWay, String coinAmount, String coinBalance) {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, String> thing4 = new HashMap<>();
        thing4.put("value", ("alipay".equals(payWay) ? "支付宝充值" : "微信充值") + "，到账" + coinAmount + "星币");
        thing4.put("color", "#173177");
        data.put("thing4", thing4);

        Map<String, String> amount5 = new HashMap<>();
        amount5.put("value", amount);
        amount5.put("color", "#173177");
        data.put("amount5", amount5);

        Map<String, String> amount6 = new HashMap<>();
        amount6.put("value", coinBalance);
        amount6.put("color", "#07C160");
        data.put("amount6", amount6);

        return data;
    }

    /**
     * 构建视频生成结果统一通知数据
     * <p>
     * 模板字段：
     * - character_string6: 工单编号
     * - thing7:           工单名称
     * - time3:            结束时间
     * - const4:           处理结果（已完成 / 失败 / 异常）
     */
    private Map<String, Object> buildVideoResultData(Long generationTaskId, String taskName,
                                                      String finishTime, String result) {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, String> character_string6 = new HashMap<>();
        character_string6.put("value", String.valueOf(generationTaskId));
        data.put("character_string6", character_string6);

        Map<String, String> thing7 = new HashMap<>();
        thing7.put("value", taskName != null && !taskName.isBlank() ? taskName : "图片生成视频");
        data.put("thing7", thing7);

        Map<String, String> time3 = new HashMap<>();
        time3.put("value", finishTime != null ? finishTime : "");
        data.put("time3", time3);

        Map<String, String> const4 = new HashMap<>();
        const4.put("value", result);
        const4.put("color", resolveResultColor(result));
        data.put("const4", const4);

        return data;
    }

    /**
     * 根据处理结果返回对应的颜色
     */
    private String resolveResultColor(String result) {
        if (result == null) {
            return "#888888";
        }
        switch (result) {
            case "已完成":
                return "#07C160";
            case "失败":
                return "#FF0000";
            case "异常":
                return "#FF6600";
            default:
                return "#888888";
        }
    }
}
