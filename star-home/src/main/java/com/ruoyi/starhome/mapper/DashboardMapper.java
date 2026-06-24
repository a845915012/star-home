package com.ruoyi.starhome.mapper;

import com.ruoyi.starhome.domain.dto.DashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据看板 Mapper
 */
@Mapper
public interface DashboardMapper {

    /**
     * 统计总用户数（未删除）
     */
    Long countTotalUsers();

    /**
     * 统计上周同期总用户数
     */
    Long countTotalUsersAtWeekAgo(@Param("weekAgo") LocalDateTime weekAgo);

    /**
     * 统计付费用户数（有充值成功记录的用户数）
     */
    Long countPaidUsers();

    /**
     * 统计普通用户数（总用户 - 付费用户）
     */
    Long countNormalUsers();

    /**
     * 统计本月充值总额（支付成功）
     */
    DashboardVO.MonthlyRevenueStat monthlyRevenue(@Param("monthStart") LocalDateTime monthStart,
                                                   @Param("monthEnd") LocalDateTime monthEnd);

    /**
     * 统计上月充值总额（支付成功）
     */
    DashboardVO.MonthlyRevenueStat lastMonthRevenue(@Param("monthStart") LocalDateTime monthStart,
                                                     @Param("monthEnd") LocalDateTime monthEnd);

    /**
     * 统计昨日新增用户数
     */
    Long countNewUsersYesterday(@Param("yesterdayStart") LocalDateTime yesterdayStart,
                                @Param("yesterdayEnd") LocalDateTime yesterdayEnd);

    /**
     * 统计前一天新增用户数
     */
    Long countNewUsersDayBefore(@Param("dayStart") LocalDateTime dayStart,
                                @Param("dayEnd") LocalDateTime dayEnd);

    /**
     * 统计场景图片生成调用总量
     */
    Long countSceneImageGen();

    /**
     * 统计文案生成调用总量
     */
    Long countCopywritingGen();

    /**
     * 统计图像生成视频调用总量
     */
    Long countImageToVideoGen();

    /**
     * 统计会员等级分布
     */
    List<DashboardVO.VipLevelStat> countVipLevelStats();
}
