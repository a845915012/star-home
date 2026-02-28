package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureMemberPackageDO;

import java.util.List;

public interface IFurnitureMemberPackageService {
    FurnitureMemberPackageDO selectFurnitureMemberPackageById(Long id);

    List<FurnitureMemberPackageDO> selectFurnitureMemberPackageList(FurnitureMemberPackageDO furnitureMemberPackage);

    int insertFurnitureMemberPackage(FurnitureMemberPackageDO furnitureMemberPackage);

    int updateFurnitureMemberPackage(FurnitureMemberPackageDO furnitureMemberPackage);

    int deleteFurnitureMemberPackageByIds(Long[] ids);

    int deleteFurnitureMemberPackageById(Long id);
}
