package com.ruoyi.starhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.vo.FurnitureRechargeOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 充值订单Mapper
 */
@Mapper
public interface FurnitureRechargeOrderMapper extends BaseMapper<FurnitureRechargeOrderDO> {

    /**
     * 条件更新订单为支付成功（仅当当前状态不是成功时更新），返回影响行数
     */
    @Update("UPDATE furniture_recharge_order SET pay_status = #{order.payStatus}, transaction_id = #{order.transactionId}, "
            + "pay_time = #{order.payTime}, notify_time = #{order.notifyTime}, notify_content = #{order.notifyContent}, "
            + "update_time = #{order.updateTime} WHERE id = #{order.id} AND pay_status != 1")
    int updatePaySuccessIfNotAlready(@Param("order") FurnitureRechargeOrderDO order);

    /**
     * 分页查询充值订单列表（含用户名和手机号）
     */
    List<FurnitureRechargeOrderVO> selectRechargeOrderListWithUser(@Param("orderNo") String orderNo,
                                                                    @Param("userId") Long userId,
                                                                    @Param("packageId") Long packageId,
                                                                    @Param("payStatus") Integer payStatus);
}
