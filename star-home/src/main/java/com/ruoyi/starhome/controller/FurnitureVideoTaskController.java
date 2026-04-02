package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.domain.dto.FurnitureVideoTaskPageRequest;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "视频任务")
@RestController
@RequestMapping("/starhome/videoTask")
public class FurnitureVideoTaskController extends BaseController {

    @Autowired
    private IFurnitureVideoTaskService furnitureVideoTaskService;

    @Operation(summary = "分页查询视频任务", description = "仅查询当前登录用户的视频任务，支持按请求描述模糊匹配")
    @Parameters({
            @Parameter(name = "prompt", description = "请求时描述（模糊匹配）", example = "现代客厅"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/page")
    public AjaxResult page(FurnitureVideoTaskPageRequest request) {
        return success(furnitureVideoTaskService.selectPage(request));
    }
}
