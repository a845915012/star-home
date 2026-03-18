package com.ruoyi.starhome.service.impl;

import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.starhome.domain.FurnitureApiCallMonitor;
import com.ruoyi.starhome.mapper.FurnitureApiCallMonitorMapper;
import com.ruoyi.starhome.service.IFurnitureApiCallMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 家居接口调用监控 服务层实现
 *
 * @author ruoyi
 */
@Service
public class FurnitureApiCallMonitorServiceImpl implements IFurnitureApiCallMonitorService {
    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    @Autowired
    private FurnitureApiCallMonitorMapper furnitureApiCallMonitorMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 查询家居接口调用监控
     *
     * @param id 主键ID
     * @return 家居接口调用监控
     */
    @Override
    public FurnitureApiCallMonitor selectFurnitureApiCallMonitorById(Long id) {
        return furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorById(id);
    }

    /**
     * 根据用户ID查询家居接口调用监控
     *
     * @param userId 用户ID
     * @return 家居接口调用监控
     */
    @Override
    public FurnitureApiCallMonitor selectFurnitureApiCallMonitorByUserId(Long userId) {
        return furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorByUserId(userId);
    }

    /**
     * 查询家居接口调用监控列表
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 家居接口调用监控
     */
    @Override
    public List<FurnitureApiCallMonitor> selectFurnitureApiCallMonitorList(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        return furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorList(furnitureApiCallMonitor);
    }

    @Override
    public TableDataInfo selectFurnitureApiCallMonitorPage(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        int pageNum = pageDomain.getPageNum() == null ? 1 : pageDomain.getPageNum();
        int pageSize = pageDomain.getPageSize() == null ? 10 : pageDomain.getPageSize();

        List<FurnitureApiCallMonitor> dbList = furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorList(furnitureApiCallMonitor);
        Map<Long, FurnitureApiCallMonitor> merged = dbList.stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(FurnitureApiCallMonitor::getUserId, item -> item, (a, b) -> a));

        LocalDateTime nowLdt = LocalDateTime.now();
        String minuteBucketKey = ApiCallMonitorCacheServiceImpl.MONITOR_MINUTE_BUCKET_PREFIX + nowLdt.format(MINUTE_FMT);
        String hourBucketKey = ApiCallMonitorCacheServiceImpl.MONITOR_HOUR_BUCKET_PREFIX + nowLdt.format(HOUR_FMT);

        Set<String> userIdSet = redisCache.getCacheSet(ApiCallMonitorCacheServiceImpl.MONITOR_USER_SET_KEY);
        if (userIdSet != null && !userIdSet.isEmpty()) {
            for (String userIdStr : userIdSet) {
                Long userId = parseUserId(userIdStr);
                if (userId == null) {
                    continue;
                }

                FurnitureApiCallMonitor monitor = merged.getOrDefault(userId, new FurnitureApiCallMonitor());
                monitor.setUserId(userId);

                int minuteCalls = (int) readBucketCount(minuteBucketKey, userIdStr);
                int hourCalls = (int) readBucketCount(hourBucketKey, userIdStr);
                monitor.setMinuteCalls(minuteCalls);
                monitor.setHourCalls(hourCalls);

                Date lastCallTime = readLastCallTime(userId);
                if (lastCallTime != null) {
                    monitor.setLastCallTime(lastCallTime);
                }

                merged.put(userId, monitor);
            }
        }

        List<FurnitureApiCallMonitor> mergedList = new ArrayList<>(merged.values());
        mergedList.sort(Comparator.comparing(FurnitureApiCallMonitor::getUserId, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int total = mergedList.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<FurnitureApiCallMonitor> pageList = mergedList.subList(fromIndex, toIndex);

        return new TableDataInfo(pageList, total);
    }

    private Long parseUserId(String userIdStr) {
        if (userIdStr == null) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long readBucketCount(String bucketKey, String userIdStr) {
        Object obj = redisCache.getCacheMapValue(bucketKey, userIdStr);
        if (obj == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Date readLastCallTime(Long userId) {
        Map<String, Object> metaMap = redisCache.getCacheMap(ApiCallMonitorCacheServiceImpl.MONITOR_USER_KEY_PREFIX + userId);
        if (metaMap == null || metaMap.isEmpty()) {
            return null;
        }
        Object lastCallTime = metaMap.get("lastCallTime");
        if (lastCallTime == null) {
            return null;
        }
        try {
            long time = Long.parseLong(String.valueOf(lastCallTime));
            return Date.from(Instant.ofEpochMilli(time));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 新增家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    @Override
    public int insertFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        return furnitureApiCallMonitorMapper.insertFurnitureApiCallMonitor(furnitureApiCallMonitor);
    }

    /**
     * 修改家居接口调用监控
     *
     * @param furnitureApiCallMonitor 家居接口调用监控
     * @return 结果
     */
    @Override
    public int updateFurnitureApiCallMonitor(FurnitureApiCallMonitor furnitureApiCallMonitor) {
        return furnitureApiCallMonitorMapper.updateFurnitureApiCallMonitor(furnitureApiCallMonitor);
    }

    /**
     * 批量删除家居接口调用监控
     *
     * @param ids 需要删除的主键ID
     * @return 结果
     */
    @Override
    public int deleteFurnitureApiCallMonitorByIds(Long[] ids) {
        return furnitureApiCallMonitorMapper.deleteFurnitureApiCallMonitorByIds(ids);
    }

    /**
     * 删除家居接口调用监控信息
     *
     * @param id 主键ID
     * @return 结果
     */
    @Override
    public int deleteFurnitureApiCallMonitorById(Long id) {
        return furnitureApiCallMonitorMapper.deleteFurnitureApiCallMonitorById(id);
    }
}
