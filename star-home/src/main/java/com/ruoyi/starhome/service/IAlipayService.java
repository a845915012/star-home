package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.dto.AlipayRechargeRequest;
import com.ruoyi.starhome.domain.dto.AlipayRechargeResponse;

import java.util.Map;

/**
 * 支付宝支付服务接口
 */
public interface IAlipayService {

    /**
     * 创建充值订单并获取支付表单
     *
     * @param request 充值请求
     * @return 支付响应(包含支付表单)
     */
    AlipayRechargeResponse createRechargeOrder(AlipayRechargeRequest request);

    /**
     * 创建H5充值订单
     *
     * @param request 充值请求
     * @return 支付响应(包含支付链接)
     */
    AlipayRechargeResponse createH5RechargeOrder(AlipayRechargeRequest request);

    /**
     * 处理支付宝异步通知
     *
     * @param params 通知参数
     * @return 处理结果: success/fail
     */
    String handleNotify(Map<String, String> params);

    /**
     * 根据订单号查询充值订单
     *
     * @param orderNo 订单号
     * @return 充值订单
     */
    FurnitureRechargeOrderDO getOrderByOrderNo(String orderNo);

    /**
     * 主动查询订单支付状态
     *
     * @param orderNo 订单号
     * @return 支付状态
     */
    Integer queryPayStatus(String orderNo);
}
