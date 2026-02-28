package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureApiCostStatDO;
import com.ruoyi.starhome.mapper.FurnitureApiCostStatMapper;
import com.ruoyi.starhome.service.IFurnitureApiCostStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureApiCostStatServiceImpl implements IFurnitureApiCostStatService {
    @Autowired
    private FurnitureApiCostStatMapper furnitureApiCostStatMapper;

    @Override
    public FurnitureApiCostStatDO selectFurnitureApiCostStatById(Long id) {
        return furnitureApiCostStatMapper.selectById(id);
    }

    @Override
    public List<FurnitureApiCostStatDO> selectFurnitureApiCostStatList(FurnitureApiCostStatDO furnitureApiCostStat) {
        return furnitureApiCostStatMapper.selectList(new LambdaQueryWrapper<FurnitureApiCostStatDO>()
                .eq(furnitureApiCostStat.getStatDate() != null, FurnitureApiCostStatDO::getStatDate, furnitureApiCostStat.getStatDate())
                .orderByDesc(FurnitureApiCostStatDO::getId));
    }

    @Override
    public int insertFurnitureApiCostStat(FurnitureApiCostStatDO furnitureApiCostStat) {
        return furnitureApiCostStatMapper.insert(furnitureApiCostStat);
    }

    @Override
    public int updateFurnitureApiCostStat(FurnitureApiCostStatDO furnitureApiCostStat) {
        return furnitureApiCostStatMapper.updateById(furnitureApiCostStat);
    }

    @Override
    public int deleteFurnitureApiCostStatByIds(Long[] ids) {
        return furnitureApiCostStatMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureApiCostStatById(Long id) {
        return furnitureApiCostStatMapper.deleteById(id);
    }
}
