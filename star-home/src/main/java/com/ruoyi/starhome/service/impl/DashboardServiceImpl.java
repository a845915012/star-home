package com.ruoyi.starhome.service.impl;

import com.ruoyi.starhome.domain.dto.DashboardVO;
import com.ruoyi.starhome.mapper.DashboardMapper;
import com.ruoyi.starhome.service.IDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 数据看板 Service 实现
 */
@Service
public class DashboardServiceImpl implements IDashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();

        // 1. 总用户数 & 与上周相差百分比
        buildUserStats(vo);

        // 2. 付费用户 & 转化率
        buildPaidUserStats(vo);

        // 3. 月收入 & 与上月相差百分比
        buildRevenueStats(vo);

        // 4. 新用户 & 与昨天比新增
        buildNewUserStats(vo);

        // 5. AI调用总量（场景图片生成、文案生成、图像生成视频）
        buildAiCallStats(vo);

        // 6. 会员等级统计
        buildVipLevelStats(vo);

        return vo;
    }

    /**
     * 构建用户统计数据
     */
    private void buildUserStats(DashboardVO vo) {
        // 总用户数
        Long totalUsers = dashboardMapper.countTotalUsers();
        vo.setTotalUsers(totalUsers != null ? totalUsers : 0L);

        // 上周同期（7天前）截止时间的用户数
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        Long weekAgoUsers = dashboardMapper.countTotalUsersAtWeekAgo(weekAgo);
        weekAgoUsers = weekAgoUsers != null ? weekAgoUsers : 0L;

        // 计算变化百分比
        if (weekAgoUsers > 0) {
            BigDecimal diff = BigDecimal.valueOf(vo.getTotalUsers())
                    .subtract(BigDecimal.valueOf(weekAgoUsers));
            BigDecimal percent = diff
                    .divide(BigDecimal.valueOf(weekAgoUsers), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setTotalUsersWeekChangePercent(percent);
        } else {
            vo.setTotalUsersWeekChangePercent(BigDecimal.ZERO);
        }
    }

    /**
     * 构建付费用户统计数据
     */
    private void buildPaidUserStats(DashboardVO vo) {
        Long paidUsers = dashboardMapper.countPaidUsers();
        vo.setPaidUsers(paidUsers != null ? paidUsers : 0L);

        Long normalUsers = dashboardMapper.countNormalUsers();
        vo.setNormalUsers(normalUsers != null ? normalUsers : 0L);

        // 转化率 = 付费用户 / (付费用户 + 普通用户) * 100%
        long total = vo.getPaidUsers() + vo.getNormalUsers();
        if (total > 0) {
            BigDecimal rate = BigDecimal.valueOf(vo.getPaidUsers())
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setConversionRate(rate);
        } else {
            vo.setConversionRate(BigDecimal.ZERO);
        }
    }

    /**
     * 构建月度收入统计数据
     */
    private void buildRevenueStats(DashboardVO vo) {
        LocalDate now = LocalDate.now();

        // 本月起止时间
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime monthEnd = now.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        DashboardVO.MonthlyRevenueStat thisMonth = dashboardMapper.monthlyRevenue(monthStart, monthEnd);
        vo.setMonthlyRevenue(thisMonth != null && thisMonth.getTotalAmount() != null
                ? thisMonth.getTotalAmount() : BigDecimal.ZERO);

        // 上月起止时间
        LocalDate lastMonth = now.minusMonths(1);
        LocalDateTime lastMonthStart = lastMonth.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime lastMonthEnd = lastMonth.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        DashboardVO.MonthlyRevenueStat prevMonth = dashboardMapper.lastMonthRevenue(lastMonthStart, lastMonthEnd);
        BigDecimal prevMonthRevenue = prevMonth != null && prevMonth.getTotalAmount() != null
                ? prevMonth.getTotalAmount() : BigDecimal.ZERO;

        // 计算变化百分比
        if (prevMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = vo.getMonthlyRevenue().subtract(prevMonthRevenue);
            BigDecimal percent = diff
                    .divide(prevMonthRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setMonthlyRevenueChangePercent(percent);
        } else {
            // 上月为0时，本月有收入则为100%增长
            vo.setMonthlyRevenueChangePercent(
                    vo.getMonthlyRevenue().compareTo(BigDecimal.ZERO) > 0
                            ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
        }
    }

    /**
     * 构建新用户统计数据
     */
    private void buildNewUserStats(DashboardVO vo) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = yesterday.atTime(LocalTime.MAX);

        Long newUsersYesterday = dashboardMapper.countNewUsersYesterday(yesterdayStart, yesterdayEnd);
        vo.setNewUsersYesterday(newUsersYesterday != null ? newUsersYesterday : 0L);

        LocalDate dayBefore = yesterday.minusDays(1);
        LocalDateTime dayBeforeStart = dayBefore.atStartOfDay();
        LocalDateTime dayBeforeEnd = dayBefore.atTime(LocalTime.MAX);

        Long newUsersDayBefore = dashboardMapper.countNewUsersDayBefore(dayBeforeStart, dayBeforeEnd);
        newUsersDayBefore = newUsersDayBefore != null ? newUsersDayBefore : 0L;

        // 与前一天相比新增数量（直接差值）
        vo.setNewUsersDiffFromPrevious(vo.getNewUsersYesterday() - newUsersDayBefore);
    }

    /**
     * 构建AI调用统计数据
     */
    private void buildAiCallStats(DashboardVO vo) {
        Long sceneImageGen = dashboardMapper.countSceneImageGen();
        vo.setSceneImageGenTotal(sceneImageGen != null ? sceneImageGen : 0L);

        Long copywritingGen = dashboardMapper.countCopywritingGen();
        vo.setCopywritingGenTotal(copywritingGen != null ? copywritingGen : 0L);

        Long imageToVideoGen = dashboardMapper.countImageToVideoGen();
        vo.setImageToVideoGenTotal(imageToVideoGen != null ? imageToVideoGen : 0L);

        // 三类AI调用总量
        vo.setAiCallTotal(vo.getSceneImageGenTotal()
                + vo.getCopywritingGenTotal()
                + vo.getImageToVideoGenTotal());
    }

    /**
     * 构建会员等级统计数据
     */
    private void buildVipLevelStats(DashboardVO vo) {
        List<DashboardVO.VipLevelStat> stats = dashboardMapper.countVipLevelStats();
        if (stats != null) {
            for (DashboardVO.VipLevelStat stat : stats) {
                stat.setLevelName(getVipLevelName(stat.getVipLevel()));
            }
        }
        vo.setVipLevelStats(stats);
    }

    /**
     * 会员等级名称映射
     */
    private String getVipLevelName(Integer level) {
        if (level == null) return "未知";
        return switch (level) {
            case 0 -> "非会员";
            case 1 -> "普通会员";
            case 2 -> "高级会员";
            case 3 -> "至尊会员";
            default -> "等级" + level;
        };
    }
}
