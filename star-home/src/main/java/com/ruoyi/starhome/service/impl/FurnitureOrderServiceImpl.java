package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureMemberPackageDO;
import com.ruoyi.starhome.domain.FurnitureOrderDO;
import com.ruoyi.starhome.domain.FurnitureUserPackageRightsDO;
import com.ruoyi.starhome.domain.dto.CreateOrderRequest;
import com.ruoyi.starhome.mapper.FurnitureMemberPackageMapper;
import com.ruoyi.starhome.mapper.FurnitureOrderMapper;
import com.ruoyi.starhome.mapper.FurnitureUserPackageRightsMapper;
import com.ruoyi.starhome.service.IFurnitureOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class FurnitureOrderServiceImpl implements IFurnitureOrderService {
    @Autowired
    private FurnitureOrderMapper furnitureOrderMapper;

    @Autowired
    private FurnitureMemberPackageMapper furnitureMemberPackageMapper;

    @Autowired
    private FurnitureUserPackageRightsMapper furnitureUserPackageRightsMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FurnitureOrderDO createOrder(CreateOrderRequest request) {
        if (request == null || request.getUserId() == null || request.getPackageId() == null) {
            throw new ServiceException("userId和packageId不能为空");
        }

        FurnitureMemberPackageDO memberPackage = furnitureMemberPackageMapper.selectById(request.getPackageId());
        if (memberPackage == null) {
            throw new ServiceException("套餐不存在");
        }
        if (memberPackage.getStatus() != null && memberPackage.getStatus() == 0) {
            throw new ServiceException("套餐已停用，无法下单");
        }

        Date now = new Date();

        // 1. 创建订单
        FurnitureOrderDO order = new FurnitureOrderDO();
        order.setOrderNo("SO" + System.currentTimeMillis());
        order.setUserId(request.getUserId());
        order.setPackageId(request.getPackageId());
        order.setAmount(memberPackage.getPrice() == null ? BigDecimal.ZERO : memberPackage.getPrice());
        order.setPayStatus(1);
        order.setPayWay("system");
        order.setPayTime(now);
        order.setRemark("下单/续费开通套餐");
        furnitureOrderMapper.insert(order);

        // 2. 套餐权益：兼容首购与续费
        FurnitureUserPackageRightsDO rights = furnitureUserPackageRightsMapper.selectOne(
                new LambdaQueryWrapper<FurnitureUserPackageRightsDO>()
                        .eq(FurnitureUserPackageRightsDO::getUserId, request.getUserId())
                        .eq(FurnitureUserPackageRightsDO::getPackageId, request.getPackageId())
                        .orderByDesc(FurnitureUserPackageRightsDO::getId)
                        .last("limit 1")
        );

        boolean unlimited = memberPackage.getIsUnlimited() != null && memberPackage.getIsUnlimited() == 1;
        int packageLimit = memberPackage.getApiCallLimit() == null ? 0 : memberPackage.getApiCallLimit();

        if (rights == null) {
            // 首购：新建权益
            rights = new FurnitureUserPackageRightsDO();
            rights.setUserId(request.getUserId());
            rights.setPackageId(request.getPackageId());
            rights.setUsedCalls(0);
            rights.setRemainingCalls(unlimited ? -1 : packageLimit);
            rights.setIsActive(1);
            rights.setBeginTime(now);
            rights.setLastResetTime(now);
            rights.setExpireTime(calculateExpireTime(now, memberPackage.getValidDays()));
            rights.setRemark("下单自动开通权益");
            furnitureUserPackageRightsMapper.insert(rights);
        } else {
            // 续费：延长有效期 + 累加次数
            rights.setIsActive(1);
            if (rights.getBeginTime() == null) {
                rights.setBeginTime(now);
            }

            if (unlimited) {
                rights.setRemainingCalls(-1);
            } else {
                int oldRemaining = rights.getRemainingCalls() == null || rights.getRemainingCalls() < 0 ? 0 : rights.getRemainingCalls();
                rights.setRemainingCalls(oldRemaining + packageLimit);
            }

            Date baseExpire = rights.getExpireTime() != null && rights.getExpireTime().after(now) ? rights.getExpireTime() : now;
            rights.setExpireTime(calculateExpireTime(baseExpire, memberPackage.getValidDays()));
            rights.setRemark("续费自动延长权益");
            furnitureUserPackageRightsMapper.updateById(rights);
        }

        return order;
    }

    private Date calculateExpireTime(Date baseTime, Integer validDays) {
        if (validDays == null) {
            return null;
        }
        return new Date(baseTime.getTime() + validDays * 24L * 60 * 60 * 1000);
    }
}
