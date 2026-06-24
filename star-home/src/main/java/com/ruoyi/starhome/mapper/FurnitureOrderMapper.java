package com.ruoyi.starhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureOrderDO;
import com.ruoyi.starhome.domain.vo.FurnitureOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FurnitureOrderMapper extends BaseMapper<FurnitureOrderDO> {

    /**
     * 分页查询订单列表（含用户名和手机号）
     */
    List<FurnitureOrderVO> selectOrderListWithUser(@Param("orderNo") String orderNo,
                                                    @Param("userId") Long userId,
                                                    @Param("packageId") Long packageId,
                                                    @Param("payStatus") Integer payStatus);
}
