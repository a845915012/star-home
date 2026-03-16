package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.starhome.domain.dto.TaskApiInvokeRequest;
import com.ruoyi.starhome.service.ITaskApiInvokeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "任务调用")
@RestController
@RequestMapping("/starhome/task")
public class TaskApiInvokeController extends BaseController {

    @Autowired
    private ITaskApiInvokeService taskApiInvokeService;

    @Operation(summary = "调用任务接口", description = "前端传入userId和apiNumber，调用AI接口（占位实现）、扣减次数并更新调用监控")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "任务调用请求",
            content = @Content(
                    schema = @Schema(implementation = TaskApiInvokeRequest.class),
                    examples = @ExampleObject(value = "{\"userId\":1001,\"apiNumber\":1}")
            )
    )
    @Log(title = "任务调用", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/invoke", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult invoke(@ModelAttribute TaskApiInvokeRequest request) {
        return success(taskApiInvokeService.invokeTaskApi(request));
    }

    @Operation(summary = "调用任务接口（Blocking）", description = "入参不变，强制 useSse=false，直接HTTP返回结果")
    @Log(title = "任务调用Blocking", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/invokeBlocking", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult invokeBlocking(@ModelAttribute TaskApiInvokeRequest request) {
        return success(taskApiInvokeService.invokeTaskApiBlocking(request));
    }

    @Operation(summary = "建立任务SSE流", description = "前端传入userId，建立任务调用的SSE连接")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("userId") Long userId) {
        return taskApiInvokeService.createStream(userId);
    }
}
