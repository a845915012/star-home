package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureOrderDO;
import com.ruoyi.starhome.domain.dto.CreateOrderRequest;
import com.ruoyi.starhome.service.IFurnitureOrderService;
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
    private IFurnitureOrderService furnitureOrderService;

    @Operation(summary = "分页查询订单列表", description = "按订单号、用户、套餐、支付状态等条件分页查询订单")
    @Parameters({
            @Parameter(name = "orderNo", description = "订单号", example = "SO20260228001"),
            @Parameter(name = "userId", description = "用户ID", example = "1001"),
            @Parameter(name = "packageId", description = "套餐ID", example = "2"),
            @Parameter(name = "payStatus", description = "支付状态（0未支付 1已支付）", example = "1"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public TableDataInfo list(FurnitureOrderDO furnitureOrder) {
        startPage();
        List<FurnitureOrderDO> list = furnitureOrderService.selectFurnitureOrderList(furnitureOrder);
        return getDataTable(list);
    }

    @Operation(summary = "查询订单详情", description = "根据主键ID查询订单详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return success(furnitureOrderService.selectFurnitureOrderById(id));
    }

    @Operation(summary = "下单", description = "前端传入用户ID和套餐ID，创建订单并自动开通权益（beginTime=当前时间）")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "下单请求对象",
            content = @Content(schema = @Schema(implementation = CreateOrderRequest.class), examples = @ExampleObject(value = "{\"userId\":1001,\"packageId\":2}"))
    )
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping("/create")
    public AjaxResult create(@RequestBody CreateOrderRequest request) {
        return success(furnitureOrderService.createOrder(request));
    }

    @Operation(summary = "新增订单", description = "新增一条订单记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "订单对象",
            content = @Content(schema = @Schema(implementation = FurnitureOrderDO.class), examples = @ExampleObject(value = "{\"orderNo\":\"SO20260228001\",\"userId\":1001,\"packageId\":2,\"amount\":299.00,\"payStatus\":0,\"payWay\":\"wechat\",\"remark\":\"待支付\"}"))
    )
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FurnitureOrderDO furnitureOrder) {
        return toAjax(furnitureOrderService.insertFurnitureOrder(furnitureOrder));
    }

    @Operation(summary = "修改订单", description = "根据主键ID修改订单记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "订单对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureOrderDO.class), examples = @ExampleObject(value = "{\"id\":1,\"payStatus\":1,\"payWay\":\"wechat\",\"transactionId\":\"WXTXN123456\",\"payTime\":\"2026-02-28 12:00:00\"}"))
    )
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FurnitureOrderDO furnitureOrder) {
        return toAjax(furnitureOrderService.updateFurnitureOrder(furnitureOrder));
    }

    @Operation(summary = "删除订单", description = "按主键ID集合批量删除订单")
    @Log(title = "订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2,3") @PathVariable Long[] ids) {
        return toAjax(furnitureOrderService.deleteFurnitureOrderByIds(ids));
    }
}
