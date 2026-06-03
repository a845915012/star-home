package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureUserPackageRightsDO;

import java.util.List;

public interface IFurnitureUserPackageRightsService {
    FurnitureUserPackageRightsDO selectFurnitureUserPackageRightsById(Long id);

    List<FurnitureUserPackageRightsDO> selectFurnitureUserPackageRightsList(FurnitureUserPackageRightsDO furnitureUserPackageRights);

    int insertFurnitureUserPackageRights(FurnitureUserPackageRightsDO furnitureUserPackageRights);

    int updateFurnitureUserPackageRights(FurnitureUserPackageRightsDO furnitureUserPackageRights);

    int deleteFurnitureUserPackageRightsByIds(Long[] ids);

    int deleteFurnitureUserPackageRightsById(Long id);
}
