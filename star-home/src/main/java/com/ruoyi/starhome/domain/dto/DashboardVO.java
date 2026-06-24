package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 数据看板 VO
 */
@Data
public class DashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 总用户数 */
    private Long totalUsers;

    /** 与上周相比变化百分比（正数表示增长） */
    private BigDecimal totalUsersWeekChangePercent;

    /** 付费用户数（有充值成功记录的用户） */
    private Long paidUsers;

    /** 普通用户总数（排除付费用户） */
    private Long normalUsers;

    /** 普通用户转付费用户转化率（%） */
    private BigDecimal conversionRate;

    /** 月收入（当月充值总额） */
    private BigDecimal monthlyRevenue;

    /** 与上月相比变化百分比 */
    private BigDecimal monthlyRevenueChangePercent;

    /** 昨日新增用户数 */
    private Long newUsersYesterday;

    /** 与前一天相比新增数量 */
    private Long newUsersDiffFromPrevious;

    /** 场景图片生成调用总量 */
    private Long sceneImageGenTotal;

    /** 文案生成调用总量 */
    private Long copywritingGenTotal;

    /** 图像生成视频调用总量 */
    private Long imageToVideoGenTotal;

    /** 三类AI调用总量 */
    private Long aiCallTotal;

    /** 会员等级统计列表 */
    private List<VipLevelStat> vipLevelStats;

    /**
     * 会员等级统计
     */
    @Data
    public static class VipLevelStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 会员等级（0=非会员, 1=普通会员, 2=高级会员等） */
        private Integer vipLevel;

        /** 等级名称 */
        private String levelName;

        /** 该等级用户数量 */
        private Long count;
    }

    /**
     * 月度收入统计（用于 MyBatis resultType 映射）
     */
    @Data
    public static class MonthlyRevenueStat implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 总收入金额 */
        private BigDecimal totalAmount;

        /** 充值笔数 */
        private Long orderCount;
    }
}
