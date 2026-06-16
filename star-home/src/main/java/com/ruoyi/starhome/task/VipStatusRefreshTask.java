package com.ruoyi.starhome.task;

import com.ruoyi.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VipStatusRefreshTask {
    private static final Logger log = LoggerFactory.getLogger(VipStatusRefreshTask.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    @Scheduled(fixedDelay = 900000)
    public void refreshVipStatus() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = sysUserMapper.refreshExpiredVip(now);
        int activeCount = sysUserMapper.refreshActiveVip(now);
        if (expiredCount > 0 || activeCount > 0) {
            log.info("刷新用户VIP状态完成: expired={}, active={}", expiredCount, activeCount);
        }
    }
}
