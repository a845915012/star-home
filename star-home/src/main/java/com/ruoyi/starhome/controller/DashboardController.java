package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.starhome.domain.dto.DashboardVO;
import com.ruoyi.starhome.service.IDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据看板 Controller
 */
@Tag(name = "数据看板")
@RestController
@RequestMapping("/starhome/dashboard")
public class DashboardController {

    @Autowired
    private IDashboardService dashboardService;

    /**
     * 获取数据看板完整信息
     * <p>
     * 包含：
     * 1. 总用户数 & 与上周相差百分比
     * 2. 付费用户数 & 普通用户转化付费用户转化率
     * 3. 月收入 & 与上月相差百分比
     * 4. 昨日新增用户 & 与前一天对比新增数量
     * 5. 场景图片生成、文案生成、图像生成视频调用总量
     * 6. 会员等级统计
     */
    @Operation(summary = "获取数据看板", description = "返回数据看板的全部统计指标，包括用户、收入、AI调用、会员等级等")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(schema = @Schema(implementation = DashboardVO.class)))
    @GetMapping
    public R<DashboardVO> getDashboard() {
        DashboardVO vo = dashboardService.getDashboard();
        return R.ok(vo);
    }
}
