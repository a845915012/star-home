package com.ruoyi.starhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureUserBalanceAccountDO;
import com.ruoyi.starhome.domain.vo.FurnitureUserBalanceAccountPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface FurnitureUserBalanceAccountMapper extends BaseMapper<FurnitureUserBalanceAccountDO> {
    List<FurnitureUserBalanceAccountPageVO> selectFurnitureUserBalanceAccountList(@Param("username") String username);

    FurnitureUserBalanceAccountDO selectFurnitureUserBalanceAccountByUserId(@Param("userId") Long userId);

    FurnitureUserBalanceAccountPageVO selectUserBalanceSummaryByUserId(@Param("userId") Long userId);

    /**
     * 原子条件扣费：仅当余额 >= amount 时扣减，返回受影响行数（1=成功，0=余额不足/账户不存在）。
     */
    @Update("UPDATE furniture_user_balance_account " +
            "SET balance = balance - #{amount}, use_balance = use_balance + #{amount}, update_time = NOW() " +
            "WHERE user_id = #{userId} AND balance >= #{amount}")
    int deductIfEnough(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子退款：加回余额、回退已用余额。
     */
    @Update("UPDATE furniture_user_balance_account " +
            "SET balance = balance + #{amount}, use_balance = use_balance - #{amount}, update_time = NOW() " +
            "WHERE user_id = #{userId}")
    int refund(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
