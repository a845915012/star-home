package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageResult;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.dto.CreateOrderRequest;
import com.ruoyi.starhome.domain.vo.FurnitureRechargeOrderVO;
import com.ruoyi.starhome.service.IFurnitureRechargeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "订单")
@RestController
@RequestMapping("/starhome/order")
public class FurnitureOrderController extends BaseController {
    @Autowired
    private IFurnitureRechargeOrderService furnitureRechargeOrderService;

    @Operation(summary = "分页查询订单列表", description = "按订单号、用户、套餐、支付状态等条件分页查询订单")
    @Parameters({
            @Parameter(name = "orderNo", description = "订单号", example = "RC20260228001"),
            @Parameter(name = "userId", description = "用户ID", example = "1001"),
            @Parameter(name = "packageId", description = "套餐ID", example = "2"),
            @Parameter(name = "payStatus", description = "支付状态（0待支付 1已支付 2失败 3关闭）", example = "1"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public PageResult<FurnitureRechargeOrderVO> list(FurnitureRechargeOrderDO furnitureRechargeOrder) {
        startPage();
        List<FurnitureRechargeOrderVO> list = furnitureRechargeOrderService.selectFurnitureRechargeOrderList(furnitureRechargeOrder);
        return getPageResult(list);
    }

    @Operation(summary = "查询订单详情", description = "根据主键ID查询订单详情")
    @GetMapping("/{id}")
    public R<FurnitureRechargeOrderDO> getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return R.ok(furnitureRechargeOrderService.selectFurnitureRechargeOrderById(id));
    }

    @Operation(summary = "下单", description = "前端传入用户ID和套餐ID，创建订单并自动开通权益（beginTime=当前时间）")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "下单请求对象",
            content = @Content(schema = @Schema(implementation = CreateOrderRequest.class), examples = @ExampleObject(value = "{\"userId\":1001,\"packageId\":2}"))
    )
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping("/create")
    public R<?> create(@RequestBody CreateOrderRequest request) {
        return R.ok(furnitureRechargeOrderService.createOrder(request));
    }

    @Operation(summary = "新增订单", description = "新增一条订单记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "订单对象",
            content = @Content(schema = @Schema(implementation = FurnitureRechargeOrderDO.class), examples = @ExampleObject(value = "{\"orderNo\":\"RC20260228001\",\"userId\":1001,\"packageId\":2,\"amount\":299.00,\"payStatus\":0,\"payWay\":\"wechat\",\"remark\":\"待支付\"}"))
    )
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody FurnitureRechargeOrderDO furnitureRechargeOrder) {
        return toR(furnitureRechargeOrderService.insertFurnitureRechargeOrder(furnitureRechargeOrder));
    }

    @Operation(summary = "修改订单", description = "根据主键ID修改订单记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "订单对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureRechargeOrderDO.class), examples = @ExampleObject(value = "{\"id\":1,\"payStatus\":1,\"payWay\":\"wechat\",\"transactionId\":\"WXTXN123456\",\"payTime\":\"2026-02-28 12:00:00\"}"))
    )
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody FurnitureRechargeOrderDO furnitureRechargeOrder) {
        return toR(furnitureRechargeOrderService.updateFurnitureRechargeOrder(furnitureRechargeOrder));
    }

    @Operation(summary = "删除订单", description = "按主键ID集合批量删除订单")
    @Log(title = "订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2,3") @PathVariable Long[] ids) {
        return toR(furnitureRechargeOrderService.deleteFurnitureRechargeOrderByIds(ids));
    }
}
