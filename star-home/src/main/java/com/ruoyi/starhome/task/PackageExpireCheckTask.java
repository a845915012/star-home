package com.ruoyi.starhome.task;

import com.ruoyi.starhome.service.IFurnitureRechargePackageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 充值套餐过期状态检查定时任务。
 * 根据活动开始/结束时间维护 is_expire 字段。
 * 执行间隔通过 starhome.package.expire-check-interval-ms 配置，默认 30 分钟。
 */
@Component
public class PackageExpireCheckTask {

    private static final Logger log = LoggerFactory.getLogger(PackageExpireCheckTask.class);

    @Autowired
    private IFurnitureRechargePackageService furnitureRechargePackageService;

    @Scheduled(fixedDelayString = "${starhome.package.expire-check-interval-ms:1800000}")
    public void checkPackageExpire() {
        try {
            int updated = furnitureRechargePackageService.refreshExpireStatus();
            if (updated > 0) {
                log.info("刷新充值套餐过期状态完成，更新 {} 条", updated);
            }
        } catch (Exception e) {
            log.error("刷新充值套餐过期状态异常", e);
        }
    }
}
