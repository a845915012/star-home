package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureUserBalanceAccountDO;
import com.ruoyi.starhome.domain.dto.FurnitureUserBalanceRecordsPageResp;
import com.ruoyi.starhome.domain.vo.FurnitureUserBalanceAccountPageVO;
import com.ruoyi.starhome.enums.ConsumeConstants;

import java.math.BigDecimal;
import java.util.List;

public interface IFurnitureUserBalanceAccountService {

    List<FurnitureUserBalanceAccountPageVO> selectFurnitureUserBalanceAccountList(String username);

    FurnitureUserBalanceAccountDO selectFurnitureUserBalanceAccountByUserId(Long userId);

    void recharge(Long userId, BigDecimal amount);

    void consume(Long userId, BigDecimal amount);

    FurnitureUserBalanceRecordsPageResp getUserBalanceRecords(Long userId);

}

