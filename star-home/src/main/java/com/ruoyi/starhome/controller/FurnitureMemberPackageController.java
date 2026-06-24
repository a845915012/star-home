package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageResult;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureMemberPackageDO;
import com.ruoyi.starhome.service.IFurnitureMemberPackageService;
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

@Tag(name = "会员套餐")
@RestController
@RequestMapping("/starhome/memberPackage")
public class FurnitureMemberPackageController extends BaseController {
    @Autowired
    private IFurnitureMemberPackageService furnitureMemberPackageService;

    @Operation(summary = "分页查询会员套餐列表", description = "按套餐名称、类型、状态等条件分页查询会员套餐")
    @Parameters({
            @Parameter(name = "packageName", description = "套餐名称", example = "企业专业版"),
            @Parameter(name = "packageType", description = "套餐类型", example = "2"),
            @Parameter(name = "status", description = "状态（1启用 0停用）", example = "1"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public PageResult<FurnitureMemberPackageDO> list(FurnitureMemberPackageDO furnitureMemberPackage) {
        startPage();
        List<FurnitureMemberPackageDO> list = furnitureMemberPackageService.selectFurnitureMemberPackageList(furnitureMemberPackage);
        return getPageResult(list);
    }

    @Operation(summary = "查询会员套餐详情", description = "根据主键ID查询会员套餐详情")
    @GetMapping("/{id}")
    public R<FurnitureMemberPackageDO> getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return R.ok(furnitureMemberPackageService.selectFurnitureMemberPackageById(id));
    }

    @Operation(summary = "新增会员套餐", description = "新增一条会员套餐记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "会员套餐对象",
            content = @Content(schema = @Schema(implementation = FurnitureMemberPackageDO.class), examples = @ExampleObject(value = "{\"packageName\":\"企业专业版\",\"packageType\":2,\"price\":299.00,\"apiCallLimit\":5000,\"isUnlimited\":0,\"validDays\":365,\"status\":1,\"remark\":\"年度套餐\"}"))
    )
    @Log(title = "会员套餐", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody FurnitureMemberPackageDO furnitureMemberPackage) {
        return toR(furnitureMemberPackageService.insertFurnitureMemberPackage(furnitureMemberPackage));
    }

    @Operation(summary = "修改会员套餐", description = "根据主键ID修改会员套餐记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "会员套餐对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureMemberPackageDO.class), examples = @ExampleObject(value = "{\"id\":1,\"price\":259.00,\"apiCallLimit\":6000,\"status\":1,\"remark\":\"活动优惠\"}"))
    )
    @Log(title = "会员套餐", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody FurnitureMemberPackageDO furnitureMemberPackage) {
        return toR(furnitureMemberPackageService.updateFurnitureMemberPackage(furnitureMemberPackage));
    }

    @Operation(summary = "删除会员套餐", description = "按主键ID集合批量删除会员套餐")
    @Log(title = "会员套餐", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2") @PathVariable Long[] ids) {
        return toR(furnitureMemberPackageService.deleteFurnitureMemberPackageByIds(ids));
    }
}
