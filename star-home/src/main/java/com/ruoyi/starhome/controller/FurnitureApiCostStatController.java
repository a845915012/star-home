package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureApiCostStatDO;
import com.ruoyi.starhome.service.IFurnitureApiCostStatService;
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

@Tag(name = "API费用明细统计")
@RestController
@RequestMapping("/starhome/apiCostStat")
public class FurnitureApiCostStatController extends BaseController {
    @Autowired
    private IFurnitureApiCostStatService furnitureApiCostStatService;

    @Operation(summary = "分页查询API费用明细统计列表", description = "按统计日期等条件分页查询 API 成本/收入/毛利统计数据")
    @Parameters({
            @Parameter(name = "statDate", description = "统计日期（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）", example = "2026-02-28"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public TableDataInfo list(FurnitureApiCostStatDO furnitureApiCostStat) {
        startPage();
        List<FurnitureApiCostStatDO> list = furnitureApiCostStatService.selectFurnitureApiCostStatList(furnitureApiCostStat);
        return getDataTable(list);
    }

    @Operation(summary = "查询API费用明细统计详情", description = "根据主键ID查询单条 API费用明细统计记录")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return success(furnitureApiCostStatService.selectFurnitureApiCostStatById(id));
    }

    @Operation(summary = "新增API费用明细统计", description = "新增一条 API费用明细统计记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "API费用明细统计对象",
            content = @Content(schema = @Schema(implementation = FurnitureApiCostStatDO.class), examples = @ExampleObject(value = "{\"statDate\":\"2026-02-28 00:00:00\",\"totalCalls\":1000,\"totalUsers\":180,\"apiCost\":256.50,\"revenue\":400.00,\"grossProfit\":143.50}"))
    )
    @Log(title = "API费用明细统计", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FurnitureApiCostStatDO furnitureApiCostStat) {
        return toAjax(furnitureApiCostStatService.insertFurnitureApiCostStat(furnitureApiCostStat));
    }

    @Operation(summary = "修改API费用明细统计", description = "根据主键ID修改 API费用明细统计记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "API费用明细统计对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureApiCostStatDO.class), examples = @ExampleObject(value = "{\"id\":1,\"totalCalls\":1100,\"totalUsers\":190,\"apiCost\":280.00,\"revenue\":430.00,\"grossProfit\":150.00}"))
    )
    @Log(title = "API费用明细统计", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FurnitureApiCostStatDO furnitureApiCostStat) {
        return toAjax(furnitureApiCostStatService.updateFurnitureApiCostStat(furnitureApiCostStat));
    }

    @Operation(summary = "删除API费用明细统计", description = "按主键ID集合批量删除 API费用明细统计记录")
    @Log(title = "API费用明细统计", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2,3") @PathVariable Long[] ids) {
        return toAjax(furnitureApiCostStatService.deleteFurnitureApiCostStatByIds(ids));
    }
}
