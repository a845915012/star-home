package com.ruoyi.starhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.vo.FurnitureAiCallRecordsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FurnitureAiCallRecordsMapper extends BaseMapper<FurnitureAiCallRecordsDO> {

    /**
     * 分页查询AI调用记录（含用户名和手机号）
     */
    List<FurnitureAiCallRecordsVO> selectPageWithUser(@Param("userId") Long userId,
                                                       @Param("startTime") java.util.Date startTime);
}
