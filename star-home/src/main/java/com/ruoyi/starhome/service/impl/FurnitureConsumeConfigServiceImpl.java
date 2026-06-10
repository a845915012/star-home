package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureConsumeConfigDO;
import com.ruoyi.starhome.mapper.FurnitureConsumeConfigMapper;
import com.ruoyi.starhome.service.IFurnitureConsumeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FurnitureConsumeConfigServiceImpl implements IFurnitureConsumeConfigService {
    @Autowired
    private FurnitureConsumeConfigMapper furnitureConsumeConfigMapper;

    @Override
    public List<FurnitureConsumeConfigDO> selectEnabledList() {
        return furnitureConsumeConfigMapper.selectList(new LambdaQueryWrapper<FurnitureConsumeConfigDO>()
                .eq(FurnitureConsumeConfigDO::getStatus, 1)
                .orderByAsc(FurnitureConsumeConfigDO::getSort)
                .orderByAsc(FurnitureConsumeConfigDO::getId));
    }

    @Override
    public FurnitureConsumeConfigDO selectEnabledByCode(String consumeCode) {
        if (consumeCode == null || consumeCode.isBlank()) {
            throw new ServiceException("consumeCode不能为空");
        }
        FurnitureConsumeConfigDO consumeConfig = furnitureConsumeConfigMapper.selectOne(
                new LambdaQueryWrapper<FurnitureConsumeConfigDO>()
                        .eq(FurnitureConsumeConfigDO::getConsumeCode, consumeCode.trim())
                        .eq(FurnitureConsumeConfigDO::getStatus, 1)
                        .last("limit 1"));
        if (consumeConfig == null) {
            throw new ServiceException("未找到启用的消费配置: " + consumeCode);
        }
        return consumeConfig;
    }
}
