package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageRequest;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;
import com.github.pagehelper.PageHelper;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsSummary;
import com.ruoyi.starhome.mapper.FurnitureAiCallRecordsMapper;
import com.ruoyi.starhome.service.IFurnitureAiCallRecordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FurnitureAiCallRecordsServiceImpl implements IFurnitureAiCallRecordsService {
    @Autowired
    private FurnitureAiCallRecordsMapper furnitureAiCallRecordsMapper;

    @Override
    public FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsList(FurnitureAiCallRecordsPageRequest request) {
        try (com.github.pagehelper.Page<Object> page = PageHelper.startPage(request.getPageNum(), request.getPageSize())) {
            List<FurnitureAiCallRecordsDO> records = furnitureAiCallRecordsMapper.selectList(new LambdaQueryWrapper<FurnitureAiCallRecordsDO>()
                    .eq(request.getUserId() != null, FurnitureAiCallRecordsDO::getUserId, request.getUserId())
                    .eq(request.getModule() != null && !request.getModule().isEmpty(), FurnitureAiCallRecordsDO::getModule, request.getModule())
                    .eq(request.getAiMode() != null && !request.getAiMode().isEmpty(), FurnitureAiCallRecordsDO::getAiMode, request.getAiMode())
                    .orderByDesc(FurnitureAiCallRecordsDO::getId));
            FurnitureAiCallRecordsPageResp resp = new FurnitureAiCallRecordsPageResp();
            resp.setList(records);
            resp.setTotal(page.getTotal());
            resp.setPageNum(page.getPageNum());
            resp.setPageSize(page.getPageSize());
            resp.setPages(page.getPages());
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
            resp.setSummary(summaries);
            return resp;
        }
    }
}
