package com.ruoyi.starhome.controller.admin;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.starhome.service.admin.IFurnitureAdminCallRecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台管理-AI调用记录管理")
@RestController
@RequestMapping("/starhome/admin/callRecords")
public class FurnitureAdminCallRecordsController extends BaseController {
    @Autowired
    private IFurnitureAdminCallRecordsService furnitureAdminCallRecordsService;

    @Operation(summary = "分页查询AI调用记录", description = "按module和username过滤分页查询AI调用记录")
    @Parameters({
            @Parameter(name = "module", description = "模块名称", example = "文生图"),
            @Parameter(name = "username", description = "用户名（模糊匹配）", example = "admin"),
            @Parameter(name = "pageNum", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10")
    })
    @GetMapping("/page")
    public R<?> page(@RequestParam(value = "module", required = false) String module,
                     @RequestParam(value = "username", required = false) String username,
                     @RequestParam("pageNum") Integer pageNum,
                     @RequestParam("pageSize") Integer pageSize) {
        return R.ok(furnitureAdminCallRecordsService.selectCallRecordsPage(module, username, pageNum, pageSize));
    }
}
