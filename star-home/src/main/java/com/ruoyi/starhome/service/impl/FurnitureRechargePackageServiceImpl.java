package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.FurnitureRechargePackageDO;
import com.ruoyi.starhome.enums.PayStatusConstants;
import com.ruoyi.starhome.mapper.FurnitureRechargeOrderMapper;
import com.ruoyi.starhome.mapper.FurnitureRechargePackageMapper;
import com.ruoyi.starhome.service.IFurnitureRechargePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureRechargePackageServiceImpl implements IFurnitureRechargePackageService {
    private static final String STATUS_ENABLED = "1";
    /** 限量无限制标识 */
    private static final int UNLIMITED = -1;

    @Autowired
    private FurnitureRechargePackageMapper furnitureRechargePackageMapper;

    @Autowired
    private FurnitureRechargeOrderMapper furnitureRechargeOrderMapper;

    @Override
    public FurnitureRechargePackageDO selectFurnitureRechargePackageById(Long id) {
        return furnitureRechargePackageMapper.selectById(id);
    }

    @Override
    public List<FurnitureRechargePackageDO> selectFurnitureRechargePackageList(FurnitureRechargePackageDO furnitureRechargePackage) {
        return furnitureRechargePackageMapper.selectList(new LambdaQueryWrapper<FurnitureRechargePackageDO>()
                .eq(furnitureRechargePackage.getId() != null, FurnitureRechargePackageDO::getId, furnitureRechargePackage.getId())
                .like(furnitureRechargePackage.getPackageName() != null && !furnitureRechargePackage.getPackageName().isEmpty(), FurnitureRechargePackageDO::getPackageName, furnitureRechargePackage.getPackageName())
                .eq(furnitureRechargePackage.getCostAmount() != null, FurnitureRechargePackageDO::getCostAmount, furnitureRechargePackage.getCostAmount())
                .eq(furnitureRechargePackage.getProvideAmount() != null, FurnitureRechargePackageDO::getProvideAmount, furnitureRechargePackage.getProvideAmount())
                .eq(furnitureRechargePackage.getIsVip() != null, FurnitureRechargePackageDO::getIsVip, furnitureRechargePackage.getIsVip())
                .eq(furnitureRechargePackage.getVipDay() != null, FurnitureRechargePackageDO::getVipDay, furnitureRechargePackage.getVipDay())
                .eq(furnitureRechargePackage.getStatus() != null && !furnitureRechargePackage.getStatus().isEmpty(), FurnitureRechargePackageDO::getStatus, furnitureRechargePackage.getStatus())
                .eq(furnitureRechargePackage.getTotalQuota() != null, FurnitureRechargePackageDO::getTotalQuota, furnitureRechargePackage.getTotalQuota())
                .eq(furnitureRechargePackage.getDailyQuota() != null, FurnitureRechargePackageDO::getDailyQuota, furnitureRechargePackage.getDailyQuota())
                .eq(furnitureRechargePackage.getIsExpire() != null, FurnitureRechargePackageDO::getIsExpire, furnitureRechargePackage.getIsExpire())
                .like(furnitureRechargePackage.getRemark() != null && !furnitureRechargePackage.getRemark().isEmpty(), FurnitureRechargePackageDO::getRemark, furnitureRechargePackage.getRemark()));
    }

    @Override
    public int insertFurnitureRechargePackage(FurnitureRechargePackageDO furnitureRechargePackage) {
        // 是否过期字段由活动时间派生，插入时自动计算
        furnitureRechargePackage.setIsExpire(computeIsExpire(furnitureRechargePackage));
        return furnitureRechargePackageMapper.insert(furnitureRechargePackage);
    }

    @Override
    public int updateFurnitureRechargePackage(FurnitureRechargePackageDO furnitureRechargePackage) {
        // 是否过期字段由活动时间派生，更新时自动重算
        furnitureRechargePackage.setIsExpire(computeIsExpire(furnitureRechargePackage));
        return furnitureRechargePackageMapper.updateById(furnitureRechargePackage);
    }

    @Override
    public int deleteFurnitureRechargePackageByIds(Long[] ids) {
        return furnitureRechargePackageMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureRechargePackageById(Long id) {
        return furnitureRechargePackageMapper.deleteById(id);
    }

    @Override
    public FurnitureRechargePackageDO selectEnabledById(Long id) {
        return furnitureRechargePackageMapper.selectOne(new LambdaQueryWrapper<FurnitureRechargePackageDO>()
                .eq(FurnitureRechargePackageDO::getId, id)
                .eq(FurnitureRechargePackageDO::getStatus, STATUS_ENABLED)
                .eq(FurnitureRechargePackageDO::getIsExpire, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FurnitureRechargePackageDO assertQuotaAvailable(Long packageId) {
        // 行锁，保证同一套餐的下单串行化，避免并发超量扣减
        FurnitureRechargePackageDO pkg = furnitureRechargePackageMapper.selectByIdForUpdate(packageId);
        if (pkg == null) {
            throw new ServiceException("充值套餐不存在");
        }
        if (!STATUS_ENABLED.equals(pkg.getStatus())) {
            throw new ServiceException("充值套餐已停用");
        }
        // 定时任务可能尚未刷新，这里再做一次活动时间校验
        if (computeIsExpire(pkg) != 1) {
            throw new ServiceException("充值套餐已过期或未开始");
        }

        // 总限量校验（-1 为无限）
        if (pkg.getTotalQuota() != null && pkg.getTotalQuota() != UNLIMITED) {
            long soldTotal = countPaidOrders(packageId, null, null);
            if (soldTotal >= pkg.getTotalQuota()) {
                throw new ServiceException("该套餐已达总限量，无法购买");
            }
        }

        // 每日限量校验（-1 为无限）
        if (pkg.getDailyQuota() != null && pkg.getDailyQuota() != UNLIMITED) {
            LocalDateTime[] todayRange = todayRange();
            long soldToday = countPaidOrders(packageId, todayRange[0], todayRange[1]);
            if (soldToday >= pkg.getDailyQuota()) {
                throw new ServiceException("该套餐今日已达限量，请明日再试");
            }
        }
        return pkg;
    }

    @Override
    public int refreshExpireStatus() {
        List<FurnitureRechargePackageDO> all = furnitureRechargePackageMapper.selectList(null);
        int updated = 0;
        for (FurnitureRechargePackageDO pkg : all) {
            Integer target = computeIsExpire(pkg);
            if (pkg.getIsExpire() == null || pkg.getIsExpire() != target) {
                FurnitureRechargePackageDO updater = new FurnitureRechargePackageDO();
                updater.setId(pkg.getId());
                updater.setIsExpire(target);
                furnitureRechargePackageMapper.updateById(updater);
                updated++;
            }
        }
        return updated;
    }

    private long countPaidOrders(Long packageId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<FurnitureRechargeOrderDO> wrapper = new LambdaQueryWrapper<FurnitureRechargeOrderDO>()
                .eq(FurnitureRechargeOrderDO::getPackageId, packageId)
                .eq(FurnitureRechargeOrderDO::getPayStatus, PayStatusConstants.SUCCESS);
        if (start != null && end != null) {
            // payTime 字段为 Date，这里以 Object 形式传入 LocalDateTime，由 MyBatis 映射为 TIMESTAMP
            wrapper.ge(FurnitureRechargeOrderDO::getPayTime, start)
                   .lt(FurnitureRechargeOrderDO::getPayTime, end);
        }
        return furnitureRechargeOrderMapper.selectCount(wrapper);
    }

    private LocalDateTime[] todayRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.with(LocalTime.MIN);
        LocalDateTime end = start.plusDays(1);
        return new LocalDateTime[]{start, end};
    }

    /**
     * 根据活动开始/结束时间计算是否过期。
     * 开始时间为空视为已开始；结束时间为空视为永不结束。
     */
    private Integer computeIsExpire(FurnitureRechargePackageDO pkg) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = pkg.getActivityStartTime();
        LocalDateTime end = pkg.getActivityEndTime();
        boolean started = start == null || !now.isBefore(start);
        boolean notEnded = end == null || now.isBefore(end);
        return (started && notEnded) ? 1 : 0;
    }
}
