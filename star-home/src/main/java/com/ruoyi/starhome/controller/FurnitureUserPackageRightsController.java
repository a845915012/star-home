package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureUserPackageRightsDO;
import com.ruoyi.starhome.service.IFurnitureUserPackageRightsService;
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

@Tag(name = "用户套餐权益")
@RestController
@RequestMapping("/starhome/userPackageRights")
public class FurnitureUserPackageRightsController extends BaseController {
    @Autowired
    private IFurnitureUserPackageRightsService furnitureUserPackageRightsService;

    @Operation(summary = "分页查询用户套餐权益列表", description = "按用户ID、套餐ID、是否激活等条件分页查询用户套餐权益")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", example = "1001"),
            @Parameter(name = "packageId", description = "套餐ID", example = "2"),
            @Parameter(name = "isActive", description = "是否激活（1是 0否）", example = "1"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public TableDataInfo list(FurnitureUserPackageRightsDO furnitureUserPackageRights) {
        startPage();
        List<FurnitureUserPackageRightsDO> list = furnitureUserPackageRightsService.selectFurnitureUserPackageRightsList(furnitureUserPackageRights);
        return getDataTable(list);
    }

    @Operation(summary = "查询用户套餐权益详情", description = "根据主键ID查询用户套餐权益详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return success(furnitureUserPackageRightsService.selectFurnitureUserPackageRightsById(id));
    }

    @Operation(summary = "新增用户套餐权益", description = "新增一条用户套餐权益记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "用户套餐权益对象",
            content = @Content(schema = @Schema(implementation = FurnitureUserPackageRightsDO.class), examples = @ExampleObject(value = "{\"userId\":1001,\"packageId\":2,\"usedCalls\":0,\"remainingCalls\":5000,\"isActive\":1,\"beginTime\":\"2026-02-28 00:00:00\",\"expireTime\":\"2027-02-28 00:00:00\"}"))
    )
    @Log(title = "用户套餐权益", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FurnitureUserPackageRightsDO furnitureUserPackageRights) {
        return toAjax(furnitureUserPackageRightsService.insertFurnitureUserPackageRights(furnitureUserPackageRights));
    }

    @Operation(summary = "修改用户套餐权益", description = "根据主键ID修改用户套餐权益记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "用户套餐权益对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureUserPackageRightsDO.class), examples = @ExampleObject(value = "{\"id\":1,\"usedCalls\":120,\"remainingCalls\":4880,\"isActive\":1,\"remark\":\"月度重置后\"}"))
    )
    @Log(title = "用户套餐权益", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FurnitureUserPackageRightsDO furnitureUserPackageRights) {
        return toAjax(furnitureUserPackageRightsService.updateFurnitureUserPackageRights(furnitureUserPackageRights));
    }

    @Operation(summary = "删除用户套餐权益", description = "按主键ID集合批量删除用户套餐权益")
    @Log(title = "用户套餐权益", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2") @PathVariable Long[] ids) {
        return toAjax(furnitureUserPackageRightsService.deleteFurnitureUserPackageRightsByIds(ids));
    }
}
