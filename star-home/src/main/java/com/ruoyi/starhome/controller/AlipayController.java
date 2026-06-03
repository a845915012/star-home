package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.dto.AlipayRechargeRequest;
import com.ruoyi.starhome.domain.dto.AlipayRechargeResponse;
import com.ruoyi.starhome.service.IAlipayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付控制器
 */
@Tag(name = "支付宝支付")
@RestController
@RequestMapping("/starhome/alipay")
public class AlipayController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AlipayController.class);

    @Autowired
    private IAlipayService alipayService;

    /**
     * PC端充值 - 返回支付表单HTML
     */
    @Operation(summary = "PC端充值", description = "创建充值订单并返回支付宝支付表单HTML，前端直接渲染即可跳转支付")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值请求参数",
            content = @Content(
                    schema = @Schema(implementation = AlipayRechargeRequest.class),
                    examples = @ExampleObject(value = "{\"userId\":1001,\"amount\":100.00,\"subject\":\"账户充值\"}")
            )
    )
    @PostMapping("/recharge/pc")
    public AjaxResult rechargePc(@RequestBody AlipayRechargeRequest request) {
        AlipayRechargeResponse response = alipayService.createRechargeOrder(request);
        return success(response);
    }

    /**
     * H5端充值 - 返回支付表单HTML
     */
    @Operation(summary = "H5端充值", description = "创建充值订单并返回支付宝H5支付表单HTML，适用于手机网站")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值请求参数",
            content = @Content(
                    schema = @Schema(implementation = AlipayRechargeRequest.class),
                    examples = @ExampleObject(value = "{\"userId\":1001,\"amount\":50.00,\"subject\":\"账户充值\"}")
            )
    )
    @PostMapping("/recharge/h5")
    public AjaxResult rechargeH5(@RequestBody AlipayRechargeRequest request) {
        AlipayRechargeResponse response = alipayService.createH5RechargeOrder(request);
        return success(response);
    }

    /**
     * 支付宝异步通知回调
     * 注意: 此接口需要配置为公网可访问，且不需要登录验证
     */
    @Anonymous
    @Operation(summary = "支付宝异步通知", description = "支付宝服务器回调通知，用于处理支付结果")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        log.info("收到支付宝异步通知");

        // 获取所有参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(entry.getKey(), valueStr.toString());
        }

        return alipayService.handleNotify(params);
    }

    /**
     * 支付宝同步回调
     * 用户支付完成后跳转的页面
     */
    @Anonymous
    @Operation(summary = "支付宝同步回调", description = "用户支付完成后跳转回来的页面，可用于前端展示支付结果")
    @GetMapping("/return")
    public AjaxResult returnUrl(@RequestParam("out_trade_no") String orderNo) {
        log.info("支付宝同步回调, 订单号: {}", orderNo);

        // 查询订单状态
        Integer payStatus = alipayService.queryPayStatus(orderNo);
        FurnitureRechargeOrderDO order = alipayService.getOrderByOrderNo(orderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payStatus", payStatus);
        result.put("amount", order != null ? order.getAmount() : null);
        result.put("payTime", order != null ? order.getPayTime() : null);

        return success(result);
    }

    /**
     * 查询订单支付状态
     */
    @Operation(summary = "查询订单支付状态", description = "根据订单号查询充值订单的支付状态")
    @Parameter(name = "orderNo", description = "充值订单号", example = "RC202601031234561234ABCD", required = true)
    @GetMapping("/query")
    public AjaxResult queryOrder(@RequestParam("orderNo") String orderNo) {
        Integer payStatus = alipayService.queryPayStatus(orderNo);
        FurnitureRechargeOrderDO order = alipayService.getOrderByOrderNo(orderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payStatus", payStatus);
        result.put("amount", order != null ? order.getAmount() : null);
        result.put("payTime", order != null ? order.getPayTime() : null);
        result.put("userId", order != null ? order.getUserId() : null);

        return success(result);
    }

    /**
     * 查询充值订单详情
     */
    @Operation(summary = "查询充值订单详情", description = "根据订单号查询充值订单完整信息")
    @Parameter(name = "orderNo", description = "充值订单号", example = "RC202601031234561234ABCD", required = true)
    @GetMapping("/order")
    public AjaxResult getOrder(@RequestParam("orderNo") String orderNo) {
        FurnitureRechargeOrderDO order = alipayService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return error("订单不存在");
        }
        return success(order);
    }
}
