package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.starhome.domain.FurnitureAiCallRecordsDO;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsOverview;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsPageResp;
import com.ruoyi.starhome.domain.dto.FurnitureAiCallRecordsSummary;
import com.ruoyi.starhome.mapper.FurnitureAiCallRecordsMapper;
import com.ruoyi.starhome.service.IFurnitureAiCallRecordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FurnitureAiCallRecordsServiceImpl implements IFurnitureAiCallRecordsService {
    private static final String TIME_RANGE_7D = "7D";
    private static final String TIME_RANGE_30D = "30D";
    private static final String TIME_RANGE_ALL = "ALL";

    @Autowired
    private FurnitureAiCallRecordsMapper furnitureAiCallRecordsMapper;

    @Override
    public FurnitureAiCallRecordsOverview selectFurnitureAiCallRecordsOverview(Long userId, String timeRange) {
        List<FurnitureAiCallRecordsDO> records = furnitureAiCallRecordsMapper.selectList(createBaseQuery(userId, timeRange));
        return buildOverview(records, timeRange);
    }

    @Override
    public FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsList(Long userId, String timeRange, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<FurnitureAiCallRecordsDO> query = createBaseQuery(userId, timeRange);

        PageHelper.startPage(pageNum, pageSize);
        List<FurnitureAiCallRecordsDO> records = furnitureAiCallRecordsMapper.selectList(query.orderByDesc(FurnitureAiCallRecordsDO::getId));
        PageInfo<FurnitureAiCallRecordsDO> pageInfo = new PageInfo<>(records);

        FurnitureAiCallRecordsPageResp resp = new FurnitureAiCallRecordsPageResp();
        resp.setList(records);
        resp.setTotal(pageInfo.getTotal());
        resp.setPageNum(pageInfo.getPageNum());
        resp.setPageSize(pageInfo.getPageSize());
        resp.setPages(pageInfo.getPages());
        return resp;
    }

    @Override
    public FurnitureAiCallRecordsPageResp selectFurnitureAiCallRecordsHistoryPage(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<FurnitureAiCallRecordsDO> query = new LambdaQueryWrapper<FurnitureAiCallRecordsDO>()
                .eq(userId != null, FurnitureAiCallRecordsDO::getUserId, userId)
                .orderByDesc(FurnitureAiCallRecordsDO::getId);

        PageHelper.startPage(pageNum, pageSize);
        List<FurnitureAiCallRecordsDO> records = furnitureAiCallRecordsMapper.selectList(query);
        PageInfo<FurnitureAiCallRecordsDO> pageInfo = new PageInfo<>(records);

        FurnitureAiCallRecordsPageResp resp = new FurnitureAiCallRecordsPageResp();
        resp.setList(records);
        resp.setTotal(pageInfo.getTotal());
        resp.setPageNum(pageInfo.getPageNum());
        resp.setPageSize(pageInfo.getPageSize());
        resp.setPages(pageInfo.getPages());
        return resp;
    }


    private LambdaQueryWrapper<FurnitureAiCallRecordsDO> createBaseQuery(Long userId, String timeRange) {
        LambdaQueryWrapper<FurnitureAiCallRecordsDO> query = new LambdaQueryWrapper<FurnitureAiCallRecordsDO>()
                .eq(userId != null, FurnitureAiCallRecordsDO::getUserId, userId);
        Date startTime = getStartTime(timeRange);
        if (startTime != null) {
            query.ge(FurnitureAiCallRecordsDO::getCreateTime, startTime);
        }
        return query;
    }

    private FurnitureAiCallRecordsOverview buildOverview(List<FurnitureAiCallRecordsDO> records, String timeRange) {
        FurnitureAiCallRecordsOverview overview = new FurnitureAiCallRecordsOverview();
        overview.setTimeRange(normalizeTimeRange(timeRange));
        overview.setTotalCount((long) records.size());
        overview.setTotalToken(sumBigDecimal(records, FurnitureAiCallRecordsDO::getTotalToken));
        overview.setTotalAmount(sumBigDecimal(records, FurnitureAiCallRecordsDO::getCost));

        Map<String, List<FurnitureAiCallRecordsDO>> grouped = records.stream()
                .collect(Collectors.groupingBy(record -> Objects.toString(record.getModule(), "UNKNOWN")));
        List<FurnitureAiCallRecordsSummary> summaries = new ArrayList<>();
        grouped.forEach((mode, list) -> {
            FurnitureAiCallRecordsSummary summary = new FurnitureAiCallRecordsSummary();
            summary.setModeName(mode);
            summary.setCount(list.size());
            summary.setTotalToken(sumBigDecimal(list, FurnitureAiCallRecordsDO::getTotalToken));
            summary.setTotalAmount(sumBigDecimal(list, FurnitureAiCallRecordsDO::getCost));
            summaries.add(summary);
        });
        overview.setSummary(summaries);
        return overview;
    }

    private BigDecimal sumBigDecimal(List<FurnitureAiCallRecordsDO> records, Function<FurnitureAiCallRecordsDO, BigDecimal> extractor) {
        return records.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Date getStartTime(String timeRange) {
        String normalized = normalizeTimeRange(timeRange);
        if (TIME_RANGE_ALL.equals(normalized)) {
            return null;
        }
        int days = TIME_RANGE_30D.equals(normalized) ? 30 : 7;
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        return Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String normalizeTimeRange(String timeRange) {
        if (timeRange == null || timeRange.isEmpty()) {
            return TIME_RANGE_7D;
        }
        String normalized = timeRange.trim().toUpperCase();
        if ("7".equals(normalized) || "7D".equals(normalized)) {
            return TIME_RANGE_7D;
        }
        if ("30".equals(normalized) || "30D".equals(normalized)) {
            return TIME_RANGE_30D;
        }
        if ("ALL".equals(normalized) || "ALL_TIME".equals(normalized)) {
            return TIME_RANGE_ALL;
        }
        return TIME_RANGE_7D;
    }
}
