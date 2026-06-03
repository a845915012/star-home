package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.starhome.domain.FurnitureApiCallMonitor;
import com.ruoyi.starhome.service.IFurnitureApiCallMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台管理-API调用监控")
@RestController
@RequestMapping("/starhome/apiCallMonitor")
public class FurnitureApiCallMonitorController extends BaseController {
    @Autowired
    private IFurnitureApiCallMonitorService furnitureApiCallMonitorService;

    @Operation(summary = "分页查询API调用监控", description = "分页查询调用监控（合并数据库与缓存数据）")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", example = "1001"),
            @Parameter(name = "isBlocked", description = "是否封禁", example = "false"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/page")
    public TableDataInfo page(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        return furnitureApiCallMonitorService.selectFurnitureApiCallMonitorPage(furnitureApiCallMonitor);
    }
}
