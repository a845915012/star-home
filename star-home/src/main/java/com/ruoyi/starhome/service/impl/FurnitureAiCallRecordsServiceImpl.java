package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.mapper.FurnitureAiCallRecordsMapper;
import com.ruoyi.starhome.service.IFurnitureAiCallRecordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureAiCallRecordsServiceImpl implements IFurnitureAiCallRecordsService {
    @Autowired
    private FurnitureAiCallRecordsMapper furnitureAiCallRecordsMapper;

    @Override
    public FurnitureAiCallRecordsDO selectFurnitureAiCallRecordsById(Long id) {
        return furnitureAiCallRecordsMapper.selectById(id);
    }

    @Override
    public List<FurnitureAiCallRecordsDO> selectFurnitureAiCallRecordsList(FurnitureAiCallRecordsDO furnitureAiCallRecords) {
        return furnitureAiCallRecordsMapper.selectList(new LambdaQueryWrapper<FurnitureAiCallRecordsDO>()
                .eq(furnitureAiCallRecords.getUserId() != null, FurnitureAiCallRecordsDO::getUserId, furnitureAiCallRecords.getUserId())
                .eq(furnitureAiCallRecords.getModule() != null && !furnitureAiCallRecords.getModule().isEmpty(), FurnitureAiCallRecordsDO::getModule, furnitureAiCallRecords.getModule())
                .eq(furnitureAiCallRecords.getAiMode() != null && !furnitureAiCallRecords.getAiMode().isEmpty(), FurnitureAiCallRecordsDO::getAiMode, furnitureAiCallRecords.getAiMode())
                .orderByDesc(FurnitureAiCallRecordsDO::getId));
    }

    @Override
    public int insertFurnitureAiCallRecords(FurnitureAiCallRecordsDO furnitureAiCallRecords) {
        return furnitureAiCallRecordsMapper.insert(furnitureAiCallRecords);
    }

    @Override
    public int updateFurnitureAiCallRecords(FurnitureAiCallRecordsDO furnitureAiCallRecords) {
        return furnitureAiCallRecordsMapper.updateById(furnitureAiCallRecords);
    }

    @Override
    public int deleteFurnitureAiCallRecordsByIds(Long[] ids) {
        return furnitureAiCallRecordsMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureAiCallRecordsById(Long id) {
        return furnitureAiCallRecordsMapper.deleteById(id);
    }
}
