package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureApiKeyPoolDO;

import java.util.List;

public interface IFurnitureApiKeyPoolService {
    FurnitureApiKeyPoolDO selectFurnitureApiKeyPoolById(Long id);

    List<FurnitureApiKeyPoolDO> selectFurnitureApiKeyPoolList(FurnitureApiKeyPoolDO furnitureApiKeyPool);

    int insertFurnitureApiKeyPool(FurnitureApiKeyPoolDO furnitureApiKeyPool);

    int updateFurnitureApiKeyPool(FurnitureApiKeyPoolDO furnitureApiKeyPool);

    int deleteFurnitureApiKeyPoolByIds(Long[] ids);

    int deleteFurnitureApiKeyPoolById(Long id);
}
