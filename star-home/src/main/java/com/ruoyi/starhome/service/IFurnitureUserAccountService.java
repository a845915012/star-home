package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureUserAccountDO;

import java.util.List;

public interface IFurnitureUserAccountService {
    FurnitureUserAccountDO selectFurnitureUserAccountByUserId(Long userId);

    List<FurnitureUserAccountDO> selectFurnitureUserAccountList(FurnitureUserAccountDO furnitureUserAccount);

    int insertFurnitureUserAccount(FurnitureUserAccountDO furnitureUserAccount);

    int updateFurnitureUserAccount(FurnitureUserAccountDO furnitureUserAccount);

    int deleteFurnitureUserAccountByUserIds(Long[] userIds);

    int deleteFurnitureUserAccountByUserId(Long userId);
}
