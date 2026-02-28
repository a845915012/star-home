package com.ruoyi.starhome.service.impl;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.starhome.service.ApiCallMonitorCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class ApiCallMonitorCacheServiceImpl implements ApiCallMonitorCacheService {

    public static final String MONITOR_USER_SET_KEY = "starhome:monitor:user:set";
    public static final String MONITOR_USER_KEY_PREFIX = "starhome:monitor:user:";
    public static final String MONITOR_MINUTE_BUCKET_PREFIX = "starhome:monitor:bucket:minute:";
    public static final String MONITOR_HOUR_BUCKET_PREFIX = "starhome:monitor:bucket:hour:";

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    @Autowired
    private RedisCache redisCache;

    @Override
    public void recordCall(Long userId) {
        if (userId == null) {
            return;
        }

        LocalDateTime nowLdt = LocalDateTime.now();
        Date now = Date.from(nowLdt.atZone(ZoneId.systemDefault()).toInstant());

        String userMonitorKey = MONITOR_USER_KEY_PREFIX + userId;
        String minuteBucketKey = MONITOR_MINUTE_BUCKET_PREFIX + nowLdt.format(MINUTE_FMT);
        String hourBucketKey = MONITOR_HOUR_BUCKET_PREFIX + nowLdt.format(HOUR_FMT);

        // 活跃用户集合
        redisCache.redisTemplate.opsForSet().add(MONITOR_USER_SET_KEY, String.valueOf(userId));

        // 用户维度元信息
        redisCache.redisTemplate.opsForHash().put(userMonitorKey, "lastCallTime", String.valueOf(now.getTime()));

        // 时间桶统计（更准确）
        redisCache.redisTemplate.opsForHash().increment(minuteBucketKey, String.valueOf(userId), 1L);
        redisCache.redisTemplate.opsForHash().increment(hourBucketKey, String.valueOf(userId), 1L);

        // TTL：分钟桶保留2小时，小时桶保留2天
        redisCache.expire(minuteBucketKey, 2, TimeUnit.HOURS);
        redisCache.expire(hourBucketKey, 2, TimeUnit.DAYS);
        redisCache.expire(userMonitorKey, 2, TimeUnit.DAYS);
        redisCache.expire(MONITOR_USER_SET_KEY, 2, TimeUnit.DAYS);
    }
}
