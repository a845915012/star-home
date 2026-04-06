package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.domain.FurnitureVideoTaskDO;
import com.ruoyi.starhome.domain.dto.FurnitureVideoGenerationTaskPageRequest;
import com.ruoyi.starhome.service.IFurnitureVideoGenerationTaskService;
import com.ruoyi.starhome.service.IFurnitureVideoTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "视频生成任务")
@RestController
@RequestMapping("/starhome/videoGenerationTask")
public class FurnitureVideoGenerationTaskController extends BaseController {

    @Autowired
    private IFurnitureVideoGenerationTaskService furnitureVideoGenerationTaskService;

    @Autowired
    private IFurnitureVideoTaskService furnitureVideoTaskService;

    @Operation(summary = "分页查询视频生成头任务", description = "仅查询当前登录用户的视频生成头任务")
    @Parameters({
            @Parameter(name = "status", description = "状态", example = "success"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/page")
    public AjaxResult page(FurnitureVideoGenerationTaskPageRequest request) {
        return success(furnitureVideoGenerationTaskService.selectPage(request));
    }

    @Operation(summary = "根据generationTaskId查询视频任务列表", description = "仅查询当前登录用户数据")
    @Parameter(name = "generationTaskId", description = "视频生成头任务ID", required = true, example = "1")
    @GetMapping("/videoTask/list")
    public AjaxResult listVideoTasksByGenerationTaskId(@RequestParam("generationTaskId") Long generationTaskId) {
        List<FurnitureVideoTaskDO> list = furnitureVideoTaskService.listByGenerationTaskId(generationTaskId);
        return success(list);
    }
}
