package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureUserPackageRightsDO;
import com.ruoyi.starhome.mapper.FurnitureUserPackageRightsMapper;
import com.ruoyi.starhome.service.IFurnitureUserPackageRightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureUserPackageRightsServiceImpl implements IFurnitureUserPackageRightsService {
    @Autowired
    private FurnitureUserPackageRightsMapper furnitureUserPackageRightsMapper;

    @Override
    public FurnitureUserPackageRightsDO selectFurnitureUserPackageRightsById(Long id) {
        return furnitureUserPackageRightsMapper.selectById(id);
    }

    @Override
    public List<FurnitureUserPackageRightsDO> selectFurnitureUserPackageRightsList(FurnitureUserPackageRightsDO furnitureUserPackageRights) {
        return furnitureUserPackageRightsMapper.selectList(new LambdaQueryWrapper<FurnitureUserPackageRightsDO>()
                .eq(furnitureUserPackageRights.getUserId() != null, FurnitureUserPackageRightsDO::getUserId, furnitureUserPackageRights.getUserId())
                .eq(furnitureUserPackageRights.getPackageId() != null, FurnitureUserPackageRightsDO::getPackageId, furnitureUserPackageRights.getPackageId())
                .eq(furnitureUserPackageRights.getIsActive() != null, FurnitureUserPackageRightsDO::getIsActive, furnitureUserPackageRights.getIsActive())
                .orderByDesc(FurnitureUserPackageRightsDO::getId));
    }

    @Override
    public int insertFurnitureUserPackageRights(FurnitureUserPackageRightsDO furnitureUserPackageRights) {
        return furnitureUserPackageRightsMapper.insert(furnitureUserPackageRights);
    }

    @Override
    public int updateFurnitureUserPackageRights(FurnitureUserPackageRightsDO furnitureUserPackageRights) {
        return furnitureUserPackageRightsMapper.updateById(furnitureUserPackageRights);
    }

    @Override
    public int deleteFurnitureUserPackageRightsByIds(Long[] ids) {
        return furnitureUserPackageRightsMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureUserPackageRightsById(Long id) {
        return furnitureUserPackageRightsMapper.deleteById(id);
    }
}
