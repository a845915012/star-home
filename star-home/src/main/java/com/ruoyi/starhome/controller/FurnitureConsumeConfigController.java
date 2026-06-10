package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.starhome.service.IFurnitureConsumeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "消费配置")
@RestController
@RequestMapping("/starhome/consumeConfig")
public class FurnitureConsumeConfigController extends BaseController {
    @Autowired
    private IFurnitureConsumeConfigService furnitureConsumeConfigService;

    @Operation(summary = "查询启用的消费配置列表", description = "返回状态为启用的消费配置，供前端展示")
    @GetMapping("/enabledList")
    public AjaxResult enabledList() {
        return success(furnitureConsumeConfigService.selectEnabledList());
    }
}
