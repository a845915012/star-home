package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureRechargePackageDO;

import java.util.List;

public interface IFurnitureRechargePackageService {
    FurnitureRechargePackageDO selectFurnitureRechargePackageById(Long id);

    List<FurnitureRechargePackageDO> selectFurnitureRechargePackageList(FurnitureRechargePackageDO furnitureRechargePackage);

    int insertFurnitureRechargePackage(FurnitureRechargePackageDO furnitureRechargePackage);

    int updateFurnitureRechargePackage(FurnitureRechargePackageDO furnitureRechargePackage);

    int deleteFurnitureRechargePackageByIds(Long[] ids);

    int deleteFurnitureRechargePackageById(Long id);

    FurnitureRechargePackageDO selectEnabledById(Long id);

    /**
     * 校验套餐是否可购买（启用、未过期、活动有效期内，且未超总/每日限量）。
     * 通过行锁串行化，避免并发超量扣减。返回已加锁的套餐对象以便后续使用。
     *
     * @param packageId 套餐ID
     * @return 套餐对象
     */
    FurnitureRechargePackageDO assertQuotaAvailable(Long packageId);

    /**
     * 刷新所有套餐的是否过期字段（根据活动开始/结束时间），返回更新条数。
     */
    int refreshExpireStatus();
}
