package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureRechargePackageDO;
import com.ruoyi.starhome.mapper.FurnitureRechargePackageMapper;
import com.ruoyi.starhome.service.IFurnitureRechargePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureRechargePackageServiceImpl implements IFurnitureRechargePackageService {
    private static final String STATUS_ENABLED = "1";

    @Autowired
    private FurnitureRechargePackageMapper furnitureRechargePackageMapper;

    @Override
    public FurnitureRechargePackageDO selectFurnitureRechargePackageById(Long id) {
        return furnitureRechargePackageMapper.selectById(id);
    }

    @Override
    public List<FurnitureRechargePackageDO> selectFurnitureRechargePackageList(FurnitureRechargePackageDO furnitureRechargePackage) {
        return furnitureRechargePackageMapper.selectList(new LambdaQueryWrapper<FurnitureRechargePackageDO>()
                .eq(furnitureRechargePackage.getId() != null, FurnitureRechargePackageDO::getId, furnitureRechargePackage.getId())
                .like(furnitureRechargePackage.getPackageName() != null && !furnitureRechargePackage.getPackageName().isEmpty(), FurnitureRechargePackageDO::getPackageName, furnitureRechargePackage.getPackageName())
                .eq(furnitureRechargePackage.getCostAmount() != null, FurnitureRechargePackageDO::getCostAmount, furnitureRechargePackage.getCostAmount())
                .eq(furnitureRechargePackage.getProvideAmount() != null, FurnitureRechargePackageDO::getProvideAmount, furnitureRechargePackage.getProvideAmount())
                .eq(furnitureRechargePackage.getIsVip() != null, FurnitureRechargePackageDO::getIsVip, furnitureRechargePackage.getIsVip())
                .eq(furnitureRechargePackage.getVipDay() != null, FurnitureRechargePackageDO::getVipDay, furnitureRechargePackage.getVipDay())
                .eq(furnitureRechargePackage.getStatus() != null && !furnitureRechargePackage.getStatus().isEmpty(), FurnitureRechargePackageDO::getStatus, furnitureRechargePackage.getStatus())
                .like(furnitureRechargePackage.getRemark() != null && !furnitureRechargePackage.getRemark().isEmpty(), FurnitureRechargePackageDO::getRemark, furnitureRechargePackage.getRemark()));
    }

    @Override
    public int insertFurnitureRechargePackage(FurnitureRechargePackageDO furnitureRechargePackage) {
        return furnitureRechargePackageMapper.insert(furnitureRechargePackage);
    }

    @Override
    public int updateFurnitureRechargePackage(FurnitureRechargePackageDO furnitureRechargePackage) {
        return furnitureRechargePackageMapper.updateById(furnitureRechargePackage);
    }

    @Override
    public int deleteFurnitureRechargePackageByIds(Long[] ids) {
        return furnitureRechargePackageMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureRechargePackageById(Long id) {
        return furnitureRechargePackageMapper.deleteById(id);
    }

    @Override
    public FurnitureRechargePackageDO selectEnabledById(Long id) {
        return furnitureRechargePackageMapper.selectOne(new LambdaQueryWrapper<FurnitureRechargePackageDO>()
                .eq(FurnitureRechargePackageDO::getId, id)
                .eq(FurnitureRechargePackageDO::getStatus, STATUS_ENABLED));
    }
}
