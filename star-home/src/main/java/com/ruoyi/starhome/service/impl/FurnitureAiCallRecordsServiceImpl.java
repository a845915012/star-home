package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsSummary;
import com.ruoyi.starhome.mapper.FurnitureAiCallRecordsMapper;
import com.ruoyi.starhome.service.IFurnitureAiCallRecordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.ruoyi.common.utils.PageUtils.startPage;

@Service
public class FurnitureAiCallRecordsServiceImpl implements IFurnitureAiCallRecordsService {
    @Autowired
    private FurnitureAiCallRecordsMapper furnitureAiCallRecordsMapper;

    @Override
    public FurnitureAiCallRecordsDO selectFurnitureAiCallRecordsById(Long id) {
        return furnitureAiCallRecordsMapper.selectById(id);
    }

    @Override
    public FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsList(FurnitureAiCallRecordsDO furnitureAiCallRecords) {
        startPage();
        List<FurnitureAiCallRecordsDO> records = furnitureAiCallRecordsMapper.selectList(new LambdaQueryWrapper<FurnitureAiCallRecordsDO>()
                .eq(furnitureAiCallRecords.getUserId() != null, FurnitureAiCallRecordsDO::getUserId, furnitureAiCallRecords.getUserId())
                .eq(furnitureAiCallRecords.getModule() != null && !furnitureAiCallRecords.getModule().isEmpty(), FurnitureAiCallRecordsDO::getModule, furnitureAiCallRecords.getModule())
                .eq(furnitureAiCallRecords.getAiMode() != null && !furnitureAiCallRecords.getAiMode().isEmpty(), FurnitureAiCallRecordsDO::getAiMode, furnitureAiCallRecords.getAiMode())
                .orderByDesc(FurnitureAiCallRecordsDO::getId));
        FurnitureAiCallRecordsPageResp resp = new FurnitureAiCallRecordsPageResp();
        resp.setList(records);
        // 按 aiMode 分组
        Map<String, List<FurnitureAiCallRecordsDO>> grouped = records.stream()
                .collect(Collectors.groupingBy(FurnitureAiCallRecordsDO::getAiMode));
        List<FurnitureAiCallRecordsSummary> summaries = new ArrayList<>();
        grouped.forEach((mode, list) -> {
            FurnitureAiCallRecordsSummary summary = new FurnitureAiCallRecordsSummary();
            summary.setModeName(mode);
            summary.setCount(list.size());
            // 汇总 totalToken（注意处理 null）
            BigDecimal totalToken = list.stream()
                    .map(FurnitureAiCallRecordsDO::getTotalToken)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalToken(totalToken);
            // 汇总 cost（注意处理 null）
            BigDecimal totalAmount = list.stream()
                    .map(FurnitureAiCallRecordsDO::getCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalAmount(totalAmount);

            summaries.add(summary);
        });
        return resp;
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
