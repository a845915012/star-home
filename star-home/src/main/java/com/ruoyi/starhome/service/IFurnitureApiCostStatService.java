package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureApiCostStatDO;

import java.util.List;

public interface IFurnitureApiCostStatService {
    FurnitureApiCostStatDO selectFurnitureApiCostStatById(Long id);

    List<FurnitureApiCostStatDO> selectFurnitureApiCostStatList(FurnitureApiCostStatDO furnitureApiCostStat);

    int insertFurnitureApiCostStat(FurnitureApiCostStatDO furnitureApiCostStat);

    int updateFurnitureApiCostStat(FurnitureApiCostStatDO furnitureApiCostStat);

    int deleteFurnitureApiCostStatByIds(Long[] ids);

    int deleteFurnitureApiCostStatById(Long id);
}
