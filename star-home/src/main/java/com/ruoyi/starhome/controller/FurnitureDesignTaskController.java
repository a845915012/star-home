package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.FurnitureDesignTaskDO;
import com.ruoyi.starhome.service.IFurnitureDesignTaskService;
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

@Tag(name = "设计任务")
@RestController
@RequestMapping("/starhome/designTask")
public class FurnitureDesignTaskController extends BaseController {
    @Autowired
    private IFurnitureDesignTaskService furnitureDesignTaskService;

    @Operation(summary = "分页查询设计任务列表", description = "按用户、状态等条件分页查询设计任务")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", example = "1001"),
            @Parameter(name = "status", description = "任务状态", example = "1"),
            @Parameter(name = "styleType", description = "风格类型", example = "modern"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/list")
    public TableDataInfo list(FurnitureDesignTaskDO furnitureDesignTask) {
        startPage();
        List<FurnitureDesignTaskDO> list = furnitureDesignTaskService.selectFurnitureDesignTaskList(furnitureDesignTask);
        return getDataTable(list);
    }

    @Operation(summary = "查询设计任务详情", description = "根据主键ID查询设计任务详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "主键ID", required = true, example = "1") @PathVariable Long id) {
        return success(furnitureDesignTaskService.selectFurnitureDesignTaskById(id));
    }

    @Operation(summary = "新增设计任务", description = "新增一条设计任务记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "设计任务对象",
            content = @Content(schema = @Schema(implementation = FurnitureDesignTaskDO.class), examples = @ExampleObject(value = "{\"userId\":1001,\"originalImageUrl\":\"https://example.com/raw.jpg\",\"styleType\":\"modern\",\"perspective\":\"front\",\"status\":0}"))
    )
    @Log(title = "设计任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody FurnitureDesignTaskDO furnitureDesignTask) {
        return toAjax(furnitureDesignTaskService.insertFurnitureDesignTask(furnitureDesignTask));
    }

    @Operation(summary = "修改设计任务", description = "根据主键ID修改设计任务记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "设计任务对象（需包含id）",
            content = @Content(schema = @Schema(implementation = FurnitureDesignTaskDO.class), examples = @ExampleObject(value = "{\"id\":1,\"status\":1,\"resultImageUrl\":\"https://example.com/result.jpg\",\"apiCost\":2.35,\"apiDuration\":3200}"))
    )
    @Log(title = "设计任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FurnitureDesignTaskDO furnitureDesignTask) {
        return toAjax(furnitureDesignTaskService.updateFurnitureDesignTask(furnitureDesignTask));
    }

    @Operation(summary = "删除设计任务", description = "按主键ID集合批量删除设计任务")
    @Log(title = "设计任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "主键ID数组，逗号分隔", required = true, example = "1,2,3") @PathVariable Long[] ids) {
        return toAjax(furnitureDesignTaskService.deleteFurnitureDesignTaskByIds(ids));
    }
}
