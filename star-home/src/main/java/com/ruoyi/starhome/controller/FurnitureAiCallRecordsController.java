package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.service.IFurnitureAiCallRecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台管理-AI调用记录")
@RestController
@RequestMapping("/starhome/aiCallRecords")
public class FurnitureAiCallRecordsController extends BaseController {
    @Autowired
    private IFurnitureAiCallRecordsService furnitureAiCallRecordsService;

    @Operation(summary = "查询AI调用记录概览", description = "按当前登录用户和时间范围查询顶部汇总统计和功能使用情况")
    @Parameters({
            @Parameter(name = "timeRange", description = "时间范围：7D=近7天，30D=近30天，ALL=全部", example = "7D")
    })
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam("timeRange") String timeRange) {
        return success(furnitureAiCallRecordsService.selectFurnitureAiCallRecordsOverview(getUserId(), timeRange));
    }

    @Operation(summary = "分页查询AI调用记录明细", description = "按当前登录用户和时间范围分页查询最近调用记录")
    @Parameters({
            @Parameter(name = "timeRange", description = "时间范围：7D=近7天，30D=近30天，ALL=全部", example = "7D"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/page")
    public AjaxResult page(@RequestParam("timeRange") String timeRange,
                           @RequestParam("pageNum") Integer pageNum,
                           @RequestParam("pageSize") Integer pageSize) {
        return success(furnitureAiCallRecordsService.selectFurnitureAiCallRecordsList(getUserId(), timeRange, pageNum, pageSize));
    }

    @Operation(summary = "分页查询历史记录", description = "查询当前登录用户的AI调用历史记录")
    @Parameters({
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/history/page")
    public AjaxResult historyPage(@RequestParam("pageNum") Integer pageNum,
                                  @RequestParam("pageSize") Integer pageSize) {
        return success(furnitureAiCallRecordsService.selectFurnitureAiCallRecordsHistoryPage(getUserId(), pageNum, pageSize));
    }
}
