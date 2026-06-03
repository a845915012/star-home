package com.ruoyi.starhome.task;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.starhome.domain.FurnitureApiCallMonitor;
import com.ruoyi.starhome.mapper.FurnitureApiCallMonitorMapper;
import com.ruoyi.starhome.service.impl.ApiCallMonitorCacheServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

@Component
public class ApiCallMonitorFlushTask {

    private static final Logger log = LoggerFactory.getLogger(ApiCallMonitorFlushTask.class);

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final int MAX_RETRY = 3;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private FurnitureApiCallMonitorMapper furnitureApiCallMonitorMapper;

    @Scheduled(fixedDelay = 60000)
    public void flushMonitorToDb() {
        Collection<String> userIds = redisCache.getCacheSet(ApiCallMonitorCacheServiceImpl.MONITOR_USER_SET_KEY);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        LocalDateTime nowLdt = LocalDateTime.now();
        Date now = Date.from(nowLdt.atZone(ZoneId.systemDefault()).toInstant());
        String minuteBucketKey = ApiCallMonitorCacheServiceImpl.MONITOR_MINUTE_BUCKET_PREFIX + nowLdt.format(MINUTE_FMT);
        String hourBucketKey = ApiCallMonitorCacheServiceImpl.MONITOR_HOUR_BUCKET_PREFIX + nowLdt.format(HOUR_FMT);

        for (String userIdStr : userIds) {
            Long userId;
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("非法userId，跳过持久化: {}", userIdStr);
                continue;
            }

            long minuteCalls = readBucketCount(minuteBucketKey, userIdStr);
            long hourCalls = readBucketCount(hourBucketKey, userIdStr);
            if (minuteCalls <= 0 && hourCalls <= 0) {
                continue;
            }

            boolean ok = persistWithRetry(userId, (int) minuteCalls, (int) hourCalls, now);
            if (!ok) {
                log.error("调用监控持久化失败，达到最大重试次数，userId={}", userId);
                continue;
            }

            // 落库成功后清零当前时间桶该用户计数
            redisCache.setCacheMapValue(minuteBucketKey, userIdStr, 0);
            redisCache.setCacheMapValue(hourBucketKey, userIdStr, 0);
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
            log.warn("时间桶计数格式异常，bucketKey={}, userId={}, value={}", bucketKey, userIdStr, obj);
            return 0L;
        }
    }

    private boolean persistWithRetry(Long userId, int minuteCalls, int hourCalls, Date now) {
        Exception lastEx = null;
        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                persistOne(userId, minuteCalls, hourCalls, now);
                if (i > 1) {
                    log.warn("调用监控重试成功，userId={}, attempt={}", userId, i);
                }
                return true;
            } catch (Exception e) {
                lastEx = e;
                log.warn("调用监控持久化失败，准备重试，userId={}, attempt={}", userId, i, e);
            }
        }
        log.error("调用监控持久化最终失败，userId={}", userId, lastEx);
        return false;
    }

    private void persistOne(Long userId, int minuteCalls, int hourCalls, Date now) {
        FurnitureApiCallMonitor monitor = furnitureApiCallMonitorMapper.selectFurnitureApiCallMonitorByUserId(userId);
        if (monitor == null) {
            monitor = new FurnitureApiCallMonitor();
            monitor.setUserId(userId);
            monitor.setMinuteCalls(minuteCalls);
            monitor.setHourCalls(hourCalls);
            monitor.setLastCallTime(now);
            monitor.setIsBlocked(false);
            monitor.setMinuteWindowStart(now);
            monitor.setHourWindowStart(now);
            furnitureApiCallMonitorMapper.insertFurnitureApiCallMonitor(monitor);
            return;
        }

        monitor.setMinuteCalls(minuteCalls);
        monitor.setHourCalls(hourCalls);
        monitor.setLastCallTime(now);
        monitor.setMinuteWindowStart(now);
        monitor.setHourWindowStart(now);
        furnitureApiCallMonitorMapper.updateFurnitureApiCallMonitor(monitor);
    }
}
