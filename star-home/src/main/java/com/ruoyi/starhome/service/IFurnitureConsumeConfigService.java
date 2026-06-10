package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureConsumeConfigDO;

import java.util.List;

public interface IFurnitureConsumeConfigService {
    List<FurnitureConsumeConfigDO> selectEnabledList();

    FurnitureConsumeConfigDO selectEnabledByCode(String consumeCode);
}
