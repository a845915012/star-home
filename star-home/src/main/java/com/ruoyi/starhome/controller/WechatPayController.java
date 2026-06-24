package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.dto.WechatPayOrderResponse;
import com.ruoyi.starhome.domain.dto.WechatPayRechargeRequest;
import com.ruoyi.starhome.service.IWechatPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付控制器
 */
@Tag(name = "微信支付")
@RestController
@RequestMapping("/starhome/wechat/pay")
public class WechatPayController {

    private static final Logger log = LoggerFactory.getLogger(WechatPayController.class);

    @Autowired
    private IWechatPayService wechatPayService;

    @Operation(summary = "JSAPI充值下单", description = "创建微信JSAPI充值订单，返回前端调起支付所需参数")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值请求参数，packageId 和 amount 二选一；JSAPI 下单需传 openId",
            content = @Content(
                    schema = @Schema(implementation = WechatPayRechargeRequest.class),
                    examples = {
                            @ExampleObject(name = "套餐充值", value = "{\"packageId\":1,\"subject\":\"账户充值\",\"openId\":\"oUpF8uMuAJO_M2pxb1Q9zNjWeS6o\"}"),
                            @ExampleObject(name = "自定义充值", value = "{\"amount\":100.00,\"subject\":\"账户充值\",\"openId\":\"oUpF8uMuAJO_M2pxb1Q9zNjWeS6o\"}")
                    }
            )
    )
    @PostMapping("/recharge/jsapi")
    public R<WechatPayOrderResponse> rechargeJsapi(@RequestBody WechatPayRechargeRequest request) {
        WechatPayOrderResponse response = wechatPayService.createJsapiRechargeOrder(request);
        log.info("rechargeJsapi response : {}", response);
        return R.ok(response);
    }

    @Operation(summary = "Native充值下单", description = "创建微信Native充值订单，返回二维码链接 codeUrl")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值请求参数，packageId 和 amount 二选一",
            content = @Content(
                    schema = @Schema(implementation = WechatPayRechargeRequest.class),
                    examples = {
                            @ExampleObject(name = "套餐充值", value = "{\"packageId\":1,\"subject\":\"账户充值\"}"),
                            @ExampleObject(name = "自定义充值", value = "{\"amount\":100.00,\"subject\":\"账户充值\"}")
                    }
            )
    )
    @PostMapping("/recharge/native")
    public R<WechatPayOrderResponse> rechargeNative(@RequestBody WechatPayRechargeRequest request) {
        WechatPayOrderResponse response = wechatPayService.createNativeRechargeOrder(request);
        return R.ok(response);
    }

    @Operation(summary = "H5充值下单", description = "创建微信H5充值订单，返回跳转链接 h5Url，在微信外浏览器中打开后跳转微信完成支付")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值请求参数，packageId 和 amount 二选一；H5支付需要传 clientIp",
            content = @Content(
                    schema = @Schema(implementation = WechatPayRechargeRequest.class),
                    examples = {
                            @ExampleObject(name = "套餐充值", value = "{\"packageId\":1,\"subject\":\"账户充值\",\"clientIp\":\"127.0.0.1\"}"),
                            @ExampleObject(name = "自定义充值", value = "{\"amount\":100.00,\"subject\":\"账户充值\",\"clientIp\":\"127.0.0.1\"}")
                    }
            )
    )
    @PostMapping("/recharge/h5")
    public R<WechatPayOrderResponse> rechargeH5(@RequestBody WechatPayRechargeRequest request) {
        WechatPayOrderResponse response = wechatPayService.createH5RechargeOrder(request);
        return R.ok(response);
    }

    @Anonymous
    @Operation(summary = "微信支付异步通知", description = "微信支付服务器回调通知，用于处理支付结果")
    @PostMapping("/notify")
    public String notify(
            @RequestBody String requestBody,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Signature") String signature) {
        return wechatPayService.handleNotify(requestBody, timestamp, nonce, serial, signature);
    }

    @Operation(summary = "查询微信充值订单支付状态", description = "根据订单号主动查询微信支付状态")
    @Parameter(name = "orderNo", description = "充值订单号", example = "RC20260618112233A1B2C3D4", required = true)
    @GetMapping("/query")
    public R<Map<String, Object>> queryOrder(@RequestParam("orderNo") String orderNo) {
        FurnitureRechargeOrderDO order = wechatPayService.queryPayStatus(orderNo);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payStatus", order.getPayStatus());
        result.put("amount", order.getAmount());
        result.put("payAmount", order.getAmount());
        result.put("provideAmount", order.getProvideAmount());
        result.put("packageId", order.getPackageId());
        result.put("payTime", order.getPayTime());
        result.put("userId", order.getUserId());
        result.put("payWay", order.getPayWay());
        result.put("transactionId", order.getTransactionId());
        return R.ok(result);
    }

    @Operation(summary = "查询微信充值订单详情", description = "根据订单号查询微信充值订单完整信息")
    @Parameter(name = "orderNo", description = "充值订单号", example = "RC20260618112233A1B2C3D4", required = true)
    @GetMapping("/order")
    public R<FurnitureRechargeOrderDO> getOrder(@RequestParam("orderNo") String orderNo) {
        FurnitureRechargeOrderDO order = wechatPayService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return R.fail("订单不存在");
        }
        return R.ok(order);
    }

}
