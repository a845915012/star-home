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
}
