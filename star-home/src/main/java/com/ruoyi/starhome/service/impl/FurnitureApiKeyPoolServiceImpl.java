package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureApiKeyPoolDO;
import com.ruoyi.starhome.mapper.FurnitureApiKeyPoolMapper;
import com.ruoyi.starhome.service.IFurnitureApiKeyPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureApiKeyPoolServiceImpl implements IFurnitureApiKeyPoolService {
    @Autowired
    private FurnitureApiKeyPoolMapper furnitureApiKeyPoolMapper;

    @Override
    public FurnitureApiKeyPoolDO selectFurnitureApiKeyPoolById(Long id) {
        return furnitureApiKeyPoolMapper.selectById(id);
    }

    @Override
    public List<FurnitureApiKeyPoolDO> selectFurnitureApiKeyPoolList(FurnitureApiKeyPoolDO furnitureApiKeyPool) {
        return furnitureApiKeyPoolMapper.selectList(new LambdaQueryWrapper<FurnitureApiKeyPoolDO>()
                .like(furnitureApiKeyPool.getProvider() != null && !furnitureApiKeyPool.getProvider().isEmpty(), FurnitureApiKeyPoolDO::getProvider, furnitureApiKeyPool.getProvider())
                .eq(furnitureApiKeyPool.getStatus() != null, FurnitureApiKeyPoolDO::getStatus, furnitureApiKeyPool.getStatus())
                .orderByDesc(FurnitureApiKeyPoolDO::getPriority)
                .orderByDesc(FurnitureApiKeyPoolDO::getId));
    }

    @Override
    public int insertFurnitureApiKeyPool(FurnitureApiKeyPoolDO furnitureApiKeyPool) {
        return furnitureApiKeyPoolMapper.insert(furnitureApiKeyPool);
    }

    @Override
    public int updateFurnitureApiKeyPool(FurnitureApiKeyPoolDO furnitureApiKeyPool) {
        return furnitureApiKeyPoolMapper.updateById(furnitureApiKeyPool);
    }

    @Override
    public int deleteFurnitureApiKeyPoolByIds(Long[] ids) {
        return furnitureApiKeyPoolMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureApiKeyPoolById(Long id) {
        return furnitureApiKeyPoolMapper.deleteById(id);
    }
}
