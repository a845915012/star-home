package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureMemberPackageDO;
import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.FurnitureUserPackageRightsDO;
import com.ruoyi.starhome.domain.dto.CreateOrderRequest;
import com.ruoyi.starhome.domain.vo.FurnitureRechargeOrderVO;
import com.ruoyi.starhome.mapper.FurnitureMemberPackageMapper;
import com.ruoyi.starhome.mapper.FurnitureRechargeOrderMapper;
import com.ruoyi.starhome.mapper.FurnitureUserPackageRightsMapper;
import com.ruoyi.starhome.service.IFurnitureRechargeOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class FurnitureRechargeOrderServiceImpl implements IFurnitureRechargeOrderService {
    @Autowired
    private FurnitureRechargeOrderMapper furnitureRechargeOrderMapper;

    @Autowired
    private FurnitureMemberPackageMapper furnitureMemberPackageMapper;

    @Autowired
    private FurnitureUserPackageRightsMapper furnitureUserPackageRightsMapper;

    @Override
    public FurnitureRechargeOrderDO selectFurnitureRechargeOrderById(Long id) {
        return furnitureRechargeOrderMapper.selectById(id);
    }

    @Override
    public List<FurnitureRechargeOrderVO> selectFurnitureRechargeOrderList(FurnitureRechargeOrderDO furnitureRechargeOrder) {
        return furnitureRechargeOrderMapper.selectRechargeOrderListWithUser(
                furnitureRechargeOrder.getOrderNo(),
                furnitureRechargeOrder.getUserId(),
                furnitureRechargeOrder.getPackageId(),
                furnitureRechargeOrder.getPayStatus());
    }

    @Override
    public int insertFurnitureRechargeOrder(FurnitureRechargeOrderDO furnitureRechargeOrder) {
        return furnitureRechargeOrderMapper.insert(furnitureRechargeOrder);
    }

    @Override
    public int updateFurnitureRechargeOrder(FurnitureRechargeOrderDO furnitureRechargeOrder) {
        return furnitureRechargeOrderMapper.updateById(furnitureRechargeOrder);
    }

    @Override
    public int deleteFurnitureRechargeOrderByIds(Long[] ids) {
        return furnitureRechargeOrderMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int deleteFurnitureRechargeOrderById(Long id) {
        return furnitureRechargeOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FurnitureRechargeOrderDO createOrder(CreateOrderRequest request) {
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

        // 1. 创建订单（使用 furniture_recharge_order 表）
        FurnitureRechargeOrderDO order = new FurnitureRechargeOrderDO();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setPackageId(request.getPackageId());
        order.setAmount(memberPackage.getPrice() == null ? BigDecimal.ZERO : memberPackage.getPrice());
        order.setProvideAmount(memberPackage.getPrice() == null ? BigDecimal.ZERO : memberPackage.getPrice());
        order.setPayStatus(1);
        order.setPayWay("system");
        order.setPayTime(now);
        order.setSubject("套餐购买");
        order.setBody("下单/续费开通套餐");
        order.setRemark("下单/续费开通套餐");
        order.setCreateTime(now);
        order.setUpdateTime(now);
        furnitureRechargeOrderMapper.insert(order);

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

    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RC" + timestamp + uuid;
    }
}
