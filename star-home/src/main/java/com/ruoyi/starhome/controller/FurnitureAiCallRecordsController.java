package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageRequest;
import com.ruoyi.starhome.service.IFurnitureAiCallRecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台管理-AI调用记录")
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
    @GetMapping("/page")
    public AjaxResult page(FurnitureAiCallRecordsPageRequest request) {
        return success(furnitureAiCallRecordsService.selectFurnitureAiCallRecordsList(request));
    }
}
