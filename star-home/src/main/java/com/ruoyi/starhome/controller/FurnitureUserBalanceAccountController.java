package com.ruoyi.starhome.controller;

import com.github.pagehelper.PageHelper;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.dto.FurnitureUserBalanceAccountPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureUserBalanceOperateRequest;
import com.ruoyi.starhome.domain.dto.FurnitureUserBalanceRecordsPageResp;
import com.ruoyi.starhome.domain.vo.FurnitureUserBalanceAccountPageVO;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台管理-用户钱包")
@RestController
@RequestMapping("/starhome/userBalanceAccount")
public class FurnitureUserBalanceAccountController extends BaseController {
    @Autowired
    private IFurnitureUserBalanceAccountService furnitureUserBalanceAccountService;

    @Operation(summary = "分页查询用户钱包列表", description = "按用户名模糊匹配分页查询用户钱包")
    @Parameters({
            @Parameter(name = "username", description = "用户名", example = "alice"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/page")
    public TableDataInfo page(FurnitureUserBalanceAccountPageRequest request) {
        try (com.github.pagehelper.Page<Object> page = PageHelper.startPage(request.getPageNum(), request.getPageSize())) {
            List<FurnitureUserBalanceAccountPageVO> list = furnitureUserBalanceAccountService.selectFurnitureUserBalanceAccountList(request.getUsername());
            return getDataTable(list);
        }
    }

    @Operation(summary = "充值", description = "仅传入userId和amount，增加余额并记录明细")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值请求",
            content = @Content(schema = @Schema(implementation = FurnitureUserBalanceOperateRequest.class), examples = @ExampleObject(value = "{\"userId\":1001,\"amount\":100.00}"))
    )
    @Log(title = "用户钱包", businessType = BusinessType.UPDATE)
    @PostMapping("/recharge")
    public AjaxResult recharge(@RequestBody FurnitureUserBalanceOperateRequest request) {
        furnitureUserBalanceAccountService.recharge(request.getUserId(), request.getAmount());
        return success();
    }

    @Operation(summary = "消费", description = "仅传入userId和amount，扣减余额并记录明细")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "消费请求",
            content = @Content(schema = @Schema(implementation = FurnitureUserBalanceOperateRequest.class), examples = @ExampleObject(value = "{\"userId\":1001,\"amount\":20.00}"))
    )
    @Log(title = "用户钱包", businessType = BusinessType.UPDATE)
    @PostMapping("/consume")
    public AjaxResult consume(@RequestBody FurnitureUserBalanceOperateRequest request) {
        furnitureUserBalanceAccountService.consume(request.getUserId(), request.getAmount());
        return success();
    }

    @Operation(summary = "查询用户余额明细", description = "传入userId，返回用户名、余额、已使用余额及明细列表")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", example = "1001", required = true)
    })
    @GetMapping("/records")
    public AjaxResult records(Long userId) {
        FurnitureUserBalanceRecordsPageResp resp = furnitureUserBalanceAccountService.getUserBalanceRecords(userId);
        return success(resp);
    }
}
