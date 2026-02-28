package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.starhome.domain.FurnitureOrderDO;
import com.ruoyi.starhome.mapper.FurnitureOrderMapper;
import com.ruoyi.starhome.service.IFurnitureOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class FurnitureOrderServiceImpl implements IFurnitureOrderService {
    @Autowired
    private FurnitureOrderMapper furnitureOrderMapper;

    @Override
    public FurnitureOrderDO selectFurnitureOrderById(Long id) {
        return furnitureOrderMapper.selectById(id);
    }

    @Override
    public List<FurnitureOrderDO> selectFurnitureOrderList(FurnitureOrderDO furnitureOrder) {
        return furnitureOrderMapper.selectList(new LambdaQueryWrapper<FurnitureOrderDO>()
                .eq(furnitureOrder.getOrderNo() != null && !furnitureOrder.getOrderNo().isEmpty(), FurnitureOrderDO::getOrderNo, furnitureOrder.getOrderNo())
                .eq(furnitureOrder.getUserId() != null, FurnitureOrderDO::getUserId, furnitureOrder.getUserId())
                .eq(furnitureOrder.getPackageId() != null, FurnitureOrderDO::getPackageId, furnitureOrder.getPackageId())
                .eq(furnitureOrder.getPayStatus() != null, FurnitureOrderDO::getPayStatus, furnitureOrder.getPayStatus())
                .orderByDesc(FurnitureOrderDO::getId));
    }

    @Override
    public int insertFurnitureOrder(FurnitureOrderDO furnitureOrder) {
        return furnitureOrderMapper.insert(furnitureOrder);
    }

    @Override
    public int updateFurnitureOrder(FurnitureOrderDO furnitureOrder) {
        return furnitureOrderMapper.updateById(furnitureOrder);
    }

    @Override
    public int deleteFurnitureOrderByIds(Long[] ids) {
        return furnitureOrderMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureOrderById(Long id) {
        return furnitureOrderMapper.deleteById(id);
    }
}
