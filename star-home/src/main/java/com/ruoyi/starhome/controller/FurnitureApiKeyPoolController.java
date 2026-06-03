package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureApiKeyPoolDO;
import com.ruoyi.starhome.service.IFurnitureApiKeyPoolService;
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

@Tag(name = "API密钥池")
@RestController
@RequestMapping("/starhome/apiKeyPool")
public class FurnitureApiKeyPoolController extends BaseController {
    @Autowired
    private IFurnitureApiKeyPoolService furnitureApiKeyPoolService;

    @Operation(summary = "分页查询API密钥池列表", description = "按服务商、状态等条件分页查询 API 密钥池")
    @Parameters({
            @Parameter(name = "provider", description = "服务商", example = "openai"),
            @Parameter(name = "status", description = "状态（1可用 0禁用）", example = "1"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public TableDataInfo list(FurnitureApiKeyPoolDO furnitureApiKeyPool) {
        startPage();
        List<FurnitureApiKeyPoolDO> list = furnitureApiKeyPoolService.selectFurnitureApiKeyPoolList(furnitureApiKeyPool);
        return getDataTable(list);
    }

    @Operation(summary = "查询API密钥池详情", description = "根据主键ID查询单条 API 密钥池记录")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return success(furnitureApiKeyPoolService.selectFurnitureApiKeyPoolById(id));
    }

    @Operation(summary = "新增API密钥池", description = "新增一条 API 密钥池记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "API密钥池对象",
            content = @Content(schema = @Schema(implementation = FurnitureApiKeyPoolDO.class), examples = @ExampleObject(value = "{\"provider\":\"openai\",\"apiKey\":\"sk-xxx\",\"secretKey\":\"sec-xxx\",\"remainingQuota\":1000,\"totalQuota\":5000,\"status\":1,\"priority\":10}"))
    )
    @Log(title = "API密钥池", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FurnitureApiKeyPoolDO furnitureApiKeyPool) {
        return toAjax(furnitureApiKeyPoolService.insertFurnitureApiKeyPool(furnitureApiKeyPool));
    }

    @Operation(summary = "修改API密钥池", description = "根据主键ID修改 API 密钥池记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "API密钥池对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureApiKeyPoolDO.class), examples = @ExampleObject(value = "{\"id\":1,\"status\":0,\"priority\":5,\"remainingQuota\":800}"))
    )
    @Log(title = "API密钥池", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FurnitureApiKeyPoolDO furnitureApiKeyPool) {
        return toAjax(furnitureApiKeyPoolService.updateFurnitureApiKeyPool(furnitureApiKeyPool));
    }

    @Operation(summary = "删除API密钥池", description = "按主键ID集合批量删除 API 密钥池记录")
    @Log(title = "API密钥池", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2") @PathVariable Long[] ids) {
        return toAjax(furnitureApiKeyPoolService.deleteFurnitureApiKeyPoolByIds(ids));
    }
}
