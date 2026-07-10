package com.ruoyi.starhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureRechargePackageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FurnitureRechargePackageMapper extends BaseMapper<FurnitureRechargePackageDO> {

    /**
     * 加行锁查询套餐，用于下单时串行化限量校验，避免并发超量扣减
     */
    @Select("SELECT * FROM furniture_recharge_package WHERE id = #{id} FOR UPDATE")
    FurnitureRechargePackageDO selectByIdForUpdate(@Param("id") Long id);
}
