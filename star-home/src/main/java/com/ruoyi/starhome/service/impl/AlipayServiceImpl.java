package com.ruoyi.starhome.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.config.AlipayConfig;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.dto.AlipayRechargeRequest;
import com.ruoyi.starhome.domain.dto.AlipayRechargeResponse;
import com.ruoyi.starhome.enums.PayStatusConstants;
import com.ruoyi.starhome.enums.PayWayConstants;
import com.ruoyi.starhome.mapper.FurnitureRechargeOrderMapper;
import com.ruoyi.starhome.service.IAlipayService;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 支付宝支付服务实现
 */
@Service
public class AlipayServiceImpl implements IAlipayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayServiceImpl.class);

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private FurnitureRechargeOrderMapper rechargeOrderMapper;

    @Autowired
    private IFurnitureUserBalanceAccountService balanceAccountService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayRechargeResponse createRechargeOrder(AlipayRechargeRequest request) {
        // 参数校验
        validateRequest(request);

        // 创建充值订单
        FurnitureRechargeOrderDO order = createOrder(request);

        // 构建支付宝请求
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

        // 构建业务参数
        Map<String, Object> bizContent = buildBizContent(order);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        try {
            alipayRequest.setBizContent(objectMapper.writeValueAsString(bizContent));
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);

            if (!response.isSuccess()) {
                log.error("支付宝下单失败: {}", response.getSubMsg());
                throw new ServiceException("支付宝下单失败: " + response.getSubMsg());
            }

            AlipayRechargeResponse result = new AlipayRechargeResponse();
            result.setOrderNo(order.getOrderNo());
            result.setPayForm(response.getBody());
            return result;

        } catch (AlipayApiException | JsonProcessingException e) {
            log.error("支付宝下单异常", e);
            throw new ServiceException("支付宝下单异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayRechargeResponse createH5RechargeOrder(AlipayRechargeRequest request) {
        // 参数校验
        validateRequest(request);

        // 创建充值订单
        FurnitureRechargeOrderDO order = createOrder(request);

        // 构建支付宝H5请求
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();
        alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

        // 构建业务参数
        Map<String, Object> bizContent = buildBizContent(order);
        bizContent.put("product_code", "QUICK_WAP_WAY");

        try {
            alipayRequest.setBizContent(objectMapper.writeValueAsString(bizContent));
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(alipayRequest);

            if (!response.isSuccess()) {
                log.error("支付宝H5下单失败: {}", response.getSubMsg());
                throw new ServiceException("支付宝H5下单失败: " + response.getSubMsg());
            }

            AlipayRechargeResponse result = new AlipayRechargeResponse();
            result.setOrderNo(order.getOrderNo());
            result.setPayForm(response.getBody());
            return result;

        } catch (AlipayApiException | JsonProcessingException e) {
            log.error("支付宝H5下单异常", e);
            throw new ServiceException("支付宝H5下单异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> params) {
        log.info("收到支付宝异步通知: {}", params);

        try {
            // 验签
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );

            if (!signVerified) {
                log.error("支付宝通知验签失败");
                return "fail";
            }

            // 获取订单号和交易状态
            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");

            // 查询订单
            FurnitureRechargeOrderDO order = getOrderByOrderNo(orderNo);
            if (order == null) {
                log.error("充值订单不存在: {}", orderNo);
                return "fail";
            }

            // 防止重复处理
            if (order.getPayStatus() == PayStatusConstants.SUCCESS) {
                log.info("订单已处理过: {}", orderNo);
                return "success";
            }

            // 处理交易状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 支付成功
                order.setPayStatus(PayStatusConstants.SUCCESS);
                order.setTransactionId(tradeNo);
                order.setPayTime(new Date());
                order.setNotifyTime(new Date());
                order.setNotifyContent(objectMapper.writeValueAsString(params));
                order.setUpdateTime(new Date());
                rechargeOrderMapper.updateById(order);

                // 调用充值接口增加用户余额
                balanceAccountService.recharge(order.getUserId(), order.getAmount());

                log.info("充值订单支付成功, 订单号: {}, 用户ID: {}, 金额: {}",
                        orderNo, order.getUserId(), order.getAmount());

            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                // 交易关闭
                order.setPayStatus(PayStatusConstants.CLOSED);
                order.setNotifyTime(new Date());
                order.setNotifyContent(objectMapper.writeValueAsString(params));
                order.setUpdateTime(new Date());
                rechargeOrderMapper.updateById(order);

                log.info("充值订单已关闭: {}", orderNo);
            }

            return "success";

        } catch (Exception e) {
            log.error("处理支付宝通知异常", e);
            return "fail";
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
    public Integer queryPayStatus(String orderNo) {
        // 先查本地订单
        FurnitureRechargeOrderDO order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        // 如果已经是终态，直接返回
        if (order.getPayStatus() == PayStatusConstants.SUCCESS ||
                order.getPayStatus() == PayStatusConstants.CLOSED) {
            return order.getPayStatus();
        }

        // 主动查询支付宝
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, String> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(objectMapper.writeValueAsString(bizContent));

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 更新本地订单状态
                    order.setPayStatus(PayStatusConstants.SUCCESS);
                    order.setTransactionId(response.getTradeNo());
                    order.setPayTime(new Date());
                    order.setUpdateTime(new Date());
                    rechargeOrderMapper.updateById(order);

                    // 调用充值接口
                    balanceAccountService.recharge(order.getUserId(), order.getAmount());

                    return PayStatusConstants.SUCCESS;
                } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                    order.setPayStatus(PayStatusConstants.CLOSED);
                    order.setUpdateTime(new Date());
                    rechargeOrderMapper.updateById(order);
                    return PayStatusConstants.CLOSED;
                }
            }
        } catch (Exception e) {
            log.error("查询支付宝订单状态异常", e);
        }

        return order.getPayStatus();
    }

    /**
     * 参数校验
     */
    private void validateRequest(AlipayRechargeRequest request) {
        if (request.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("充值金额必须大于0");
        }
        if (request.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            throw new ServiceException("单笔充值金额不能超过50000元");
        }
    }

    /**
     * 创建充值订单
     */
    private FurnitureRechargeOrderDO createOrder(AlipayRechargeRequest request) {
        FurnitureRechargeOrderDO order = new FurnitureRechargeOrderDO();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setPayStatus(PayStatusConstants.PENDING);
        order.setPayWay(PayWayConstants.ALIPAY);
        order.setSubject(request.getSubject() != null ? request.getSubject() : "账户充值");
        order.setBody(request.getBody() != null ? request.getBody() :
                "用户余额充值" + request.getAmount() + "元");
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        rechargeOrderMapper.insert(order);
        return order;
    }

    /**
     * 构建支付宝业务参数
     */
    private Map<String, Object> buildBizContent(FurnitureRechargeOrderDO order) {
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getAmount().toString());
        bizContent.put("subject", order.getSubject());
        bizContent.put("body", order.getBody());
        return bizContent;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RC" + timestamp + uuid;
    }
}
