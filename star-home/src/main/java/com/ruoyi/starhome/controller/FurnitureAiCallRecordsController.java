package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;
import com.ruoyi.starhome.service.IFurnitureAiCallRecordsService;
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

@Tag(name = "AI调用记录")
@RestController
@RequestMapping("/starhome/aiCallRecords")
public class FurnitureAiCallRecordsController extends BaseController {
    @Autowired
    private IFurnitureAiCallRecordsService furnitureAiCallRecordsService;

    @Operation(summary = "分页查询AI调用记录列表", description = "按用户ID、模块、AI模型等条件分页查询AI调用记录")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", example = "1001"),
            @Parameter(name = "module", description = "模块", example = "draw"),
            @Parameter(name = "aiMode", description = "AI模型", example = "gpt-4o"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public AjaxResult list(FurnitureAiCallRecordsDO furnitureAiCallRecords) {
        return success(furnitureAiCallRecordsService.selectFurnitureAiCallRecordsList(furnitureAiCallRecords));
    }

    @Operation(summary = "查询AI调用记录详情", description = "根据主键ID查询单条AI调用记录")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return success(furnitureAiCallRecordsService.selectFurnitureAiCallRecordsById(id));
    }

    @Operation(summary = "新增AI调用记录", description = "新增一条AI调用记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "AI调用记录对象",
            content = @Content(schema = @Schema(implementation = FurnitureAiCallRecordsDO.class), examples = @ExampleObject(value = "{\"userId\":1001,\"module\":\"draw\",\"aiMode\":\"gpt-4o\",\"tokenIn\":1200,\"tokenOut\":600,\"totalToken\":1800,\"cost\":0.123456,\"createTime\":\"2026-03-03 10:00:00\"}"))
    )
    @Log(title = "AI调用记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FurnitureAiCallRecordsDO furnitureAiCallRecords) {
        return toAjax(furnitureAiCallRecordsService.insertFurnitureAiCallRecords(furnitureAiCallRecords));
    }

    @Operation(summary = "修改AI调用记录", description = "根据主键ID修改AI调用记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "AI调用记录对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureAiCallRecordsDO.class), examples = @ExampleObject(value = "{\"id\":1,\"module\":\"render\",\"aiMode\":\"gpt-4.1\",\"tokenIn\":1300,\"tokenOut\":700,\"totalToken\":2000,\"cost\":0.145600}"))
    )
    @Log(title = "AI调用记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FurnitureAiCallRecordsDO furnitureAiCallRecords) {
        return toAjax(furnitureAiCallRecordsService.updateFurnitureAiCallRecords(furnitureAiCallRecords));
    }

    @Operation(summary = "删除AI调用记录", description = "按主键ID集合批量删除AI调用记录")
    @Log(title = "AI调用记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2,3") @PathVariable Long[] ids) {
        return toAjax(furnitureAiCallRecordsService.deleteFurnitureAiCallRecordsByIds(ids));
    }
}
