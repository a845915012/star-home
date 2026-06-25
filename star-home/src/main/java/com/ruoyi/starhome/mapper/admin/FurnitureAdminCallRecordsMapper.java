package com.ruoyi.starhome.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.vo.FurnitureAiCallRecordsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FurnitureAdminCallRecordsMapper extends BaseMapper<FurnitureAiCallRecordsDO> {

    /**
     * 分页查询AI调用记录（含用户名和手机号），按module和username过滤
     */
    List<FurnitureAiCallRecordsVO> selectPageWithUserAndModule(@Param("module") String module,
                                                                @Param("username") String username);
}
