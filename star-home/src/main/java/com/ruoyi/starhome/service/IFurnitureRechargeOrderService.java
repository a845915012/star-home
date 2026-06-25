package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureRechargeOrderDO;
import com.ruoyi.starhome.domain.dto.CreateOrderRequest;
import com.ruoyi.starhome.domain.vo.FurnitureRechargeOrderVO;

import java.util.List;

public interface IFurnitureRechargeOrderService {
    FurnitureRechargeOrderDO selectFurnitureRechargeOrderById(Long id);

    List<FurnitureRechargeOrderVO> selectFurnitureRechargeOrderList(FurnitureRechargeOrderDO furnitureRechargeOrder);

    int insertFurnitureRechargeOrder(FurnitureRechargeOrderDO furnitureRechargeOrder);

    int updateFurnitureRechargeOrder(FurnitureRechargeOrderDO furnitureRechargeOrder);

    int deleteFurnitureRechargeOrderByIds(Long[] ids);

    int deleteFurnitureRechargeOrderById(Long id);

    FurnitureRechargeOrderDO createOrder(CreateOrderRequest request);
}
