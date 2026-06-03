package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureUserAccountDO;
import com.ruoyi.starhome.mapper.FurnitureUserAccountMapper;
import com.ruoyi.starhome.service.IFurnitureUserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureUserAccountServiceImpl implements IFurnitureUserAccountService {
    @Autowired
    private FurnitureUserAccountMapper furnitureUserAccountMapper;

    @Override
    public FurnitureUserAccountDO selectFurnitureUserAccountByUserId(Long userId) {
        return furnitureUserAccountMapper.selectById(userId);
    }

    @Override
    public List<FurnitureUserAccountDO> selectFurnitureUserAccountList(FurnitureUserAccountDO furnitureUserAccount) {
        return furnitureUserAccountMapper.selectList(new LambdaQueryWrapper<FurnitureUserAccountDO>()
                .like(furnitureUserAccount.getUsername() != null && !furnitureUserAccount.getUsername().isEmpty(), FurnitureUserAccountDO::getUsername, furnitureUserAccount.getUsername())
                .like(furnitureUserAccount.getCompany() != null && !furnitureUserAccount.getCompany().isEmpty(), FurnitureUserAccountDO::getCompany, furnitureUserAccount.getCompany())
                .eq(furnitureUserAccount.getStatus() != null, FurnitureUserAccountDO::getStatus, furnitureUserAccount.getStatus())
                .orderByDesc(FurnitureUserAccountDO::getUserId));
    }

    @Override
    public int insertFurnitureUserAccount(FurnitureUserAccountDO furnitureUserAccount) {
        return furnitureUserAccountMapper.insert(furnitureUserAccount);
    }

    @Override
    public int updateFurnitureUserAccount(FurnitureUserAccountDO furnitureUserAccount) {
        return furnitureUserAccountMapper.updateById(furnitureUserAccount);
    }

    @Override
    public int deleteFurnitureUserAccountByUserIds(Long[] userIds) {
        return furnitureUserAccountMapper.deleteByIds(Arrays.asList(userIds));
    }

    @Override
    public int deleteFurnitureUserAccountByUserId(Long userId) {
        return furnitureUserAccountMapper.deleteById(userId);
    }
}
