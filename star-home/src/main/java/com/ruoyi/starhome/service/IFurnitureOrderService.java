package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureOrderDO;
import com.ruoyi.starhome.domain.dto.CreateOrderRequest;
import com.ruoyi.starhome.domain.vo.FurnitureOrderVO;

import java.util.List;

public interface IFurnitureOrderService {
    FurnitureOrderDO selectFurnitureOrderById(Long id);

    List<FurnitureOrderVO> selectFurnitureOrderList(FurnitureOrderDO furnitureOrder);

    int insertFurnitureOrder(FurnitureOrderDO furnitureOrder);

    int updateFurnitureOrder(FurnitureOrderDO furnitureOrder);

    int deleteFurnitureOrderByIds(Long[] ids);

    int deleteFurnitureOrderById(Long id);

    FurnitureOrderDO createOrder(CreateOrderRequest request);
}
