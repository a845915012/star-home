package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageResult;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureRechargePackageDO;
import com.ruoyi.starhome.service.IFurnitureRechargePackageService;
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

@Tag(name = "充值套餐")
@RestController
@RequestMapping("/starhome/rechargePackage")
public class FurnitureRechargePackageController extends BaseController {
    @Autowired
    private IFurnitureRechargePackageService furnitureRechargePackageService;

    @Operation(summary = "分页查询充值套餐列表", description = "按花费金额、提供额度、状态等条件分页查询充值套餐")
    @Parameters({
            @Parameter(name = "packageName", description = "套餐名称", example = "新人礼包"),
            @Parameter(name = "costAmount", description = "花费金额", example = "100.00"),
            @Parameter(name = "provideAmount", description = "提供额度", example = "120.00"),
            @Parameter(name = "isVip", description = "是否VIP套餐（0否 1是）", example = "1"),
            @Parameter(name = "status", description = "状态（1启用 0停用）", example = "1"),
            @Parameter(name = "totalQuota", description = "总限量（-1无限）", example = "-1"),
            @Parameter(name = "dailyQuota", description = "每日限量（-1无限）", example = "-1"),
            @Parameter(name = "isExpire", description = "是否过期（1未过期 0过期）", example = "1"),
            @Parameter(name = "remark", description = "备注", example = "限时活动"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public PageResult<FurnitureRechargePackageDO> list(FurnitureRechargePackageDO furnitureRechargePackage) {
        startPage();
        List<FurnitureRechargePackageDO> list = furnitureRechargePackageService.selectFurnitureRechargePackageList(furnitureRechargePackage);
        return getPageResult(list);
    }

    @Operation(summary = "查询充值套餐详情", description = "根据主键ID查询充值套餐详情")
    @GetMapping("/{id}")
    public R<FurnitureRechargePackageDO> getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return R.ok(furnitureRechargePackageService.selectFurnitureRechargePackageById(id));
    }

    @Operation(summary = "新增充值套餐", description = "新增一条充值套餐记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值套餐对象",
            content = @Content(schema = @Schema(implementation = FurnitureRechargePackageDO.class), examples = @ExampleObject(value = "{\"packageName\":\"新人礼包\",\"costAmount\":100.00,\"provideAmount\":120.00,\"isVip\":1,\"vipDay\":30,\"status\":\"1\",\"remark\":\"限时活动\"}"))
    )
    @Log(title = "充值套餐", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody FurnitureRechargePackageDO furnitureRechargePackage) {
        return toR(furnitureRechargePackageService.insertFurnitureRechargePackage(furnitureRechargePackage));
    }

    @Operation(summary = "修改充值套餐", description = "根据主键ID修改充值套餐记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "充值套餐对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureRechargePackageDO.class), examples = @ExampleObject(value = "{\"id\":1,\"packageName\":\"新人礼包\",\"costAmount\":99.00,\"provideAmount\":120.00,\"isVip\":1,\"vipDay\":30,\"status\":\"1\",\"remark\":\"限时活动\"}"))
    )
    @Log(title = "充值套餐", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody FurnitureRechargePackageDO furnitureRechargePackage) {
        return toR(furnitureRechargePackageService.updateFurnitureRechargePackage(furnitureRechargePackage));
    }

    @Operation(summary = "删除充值套餐", description = "按主键ID集合批量删除充值套餐")
    @Log(title = "充值套餐", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2") @PathVariable Long[] ids) {
        return toR(furnitureRechargePackageService.deleteFurnitureRechargePackageByIds(ids));
    }
}
