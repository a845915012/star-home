package com.ruoyi.starhome.service;

import com.ruoyi.starhome.domain.FurnitureUserBalanceAccountDO;
import com.ruoyi.starhome.domain.dto.FurnitureUserBalanceRecordsPageResp;
import com.ruoyi.starhome.domain.vo.FurnitureUserBalanceAccountPageVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IFurnitureUserBalanceAccountService {

    List<FurnitureUserBalanceAccountPageVO> selectFurnitureUserBalanceAccountList(String username);

    FurnitureUserBalanceAccountDO selectFurnitureUserBalanceAccountByUserId(Long userId);

    void recharge(Long userId, BigDecimal amount);

    void consume(Long userId, BigDecimal amount);

    FurnitureUserBalanceRecordsPageResp getUserBalanceRecords(Long userId, Integer type, Integer pageNum, Integer pageSize);
    Map<String, Object> getUserBalance(Long userId);

}

