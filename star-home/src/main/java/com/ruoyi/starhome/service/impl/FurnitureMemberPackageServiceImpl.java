package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureMemberPackageDO;
import com.ruoyi.starhome.mapper.FurnitureMemberPackageMapper;
import com.ruoyi.starhome.service.IFurnitureMemberPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureMemberPackageServiceImpl implements IFurnitureMemberPackageService {
    @Autowired
    private FurnitureMemberPackageMapper furnitureMemberPackageMapper;

    @Override
    public FurnitureMemberPackageDO selectFurnitureMemberPackageById(Long id) {
        return furnitureMemberPackageMapper.selectById(id);
    }

    @Override
    public List<FurnitureMemberPackageDO> selectFurnitureMemberPackageList(FurnitureMemberPackageDO furnitureMemberPackage) {
        return furnitureMemberPackageMapper.selectList(new LambdaQueryWrapper<FurnitureMemberPackageDO>()
                .like(furnitureMemberPackage.getPackageName() != null && !furnitureMemberPackage.getPackageName().isEmpty(), FurnitureMemberPackageDO::getPackageName, furnitureMemberPackage.getPackageName())
                .eq(furnitureMemberPackage.getPackageType() != null, FurnitureMemberPackageDO::getPackageType, furnitureMemberPackage.getPackageType())
                .eq(furnitureMemberPackage.getStatus() != null, FurnitureMemberPackageDO::getStatus, furnitureMemberPackage.getStatus())
                .orderByDesc(FurnitureMemberPackageDO::getId));
    }

    @Override
    public int insertFurnitureMemberPackage(FurnitureMemberPackageDO furnitureMemberPackage) {
        return furnitureMemberPackageMapper.insert(furnitureMemberPackage);
    }

    @Override
    public int updateFurnitureMemberPackage(FurnitureMemberPackageDO furnitureMemberPackage) {
        return furnitureMemberPackageMapper.updateById(furnitureMemberPackage);
    }

    @Override
    public int deleteFurnitureMemberPackageByIds(Long[] ids) {
        return furnitureMemberPackageMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureMemberPackageById(Long id) {
        return furnitureMemberPackageMapper.deleteById(id);
    }
}
