package com.ruoyi.starhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureUserBalanceAccountDO;
import com.ruoyi.starhome.domain.vo.FurnitureUserBalanceAccountPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FurnitureUserBalanceAccountMapper extends BaseMapper<FurnitureUserBalanceAccountDO> {
    List<FurnitureUserBalanceAccountPageVO> selectFurnitureUserBalanceAccountList(@Param("username") String username);

    FurnitureUserBalanceAccountDO selectFurnitureUserBalanceAccountByUserId(@Param("userId") Long userId);

    FurnitureUserBalanceAccountPageVO selectUserBalanceSummaryByUserId(@Param("userId") Long userId);
}
