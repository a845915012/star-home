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

    /**
     * 充值（带支付方式，用于区分微信/支付宝充值记录备注）
     * @param userId 用户ID
     * @param amount 到账星币数
     * @param payWay 支付方式（wechat / alipay）
     */
    void recharge(Long userId, BigDecimal amount, String payWay);

    void consume(Long userId, BigDecimal amount);

    FurnitureUserBalanceRecordsPageResp getUserBalanceRecords(Long userId, Integer type, Integer pageNum, Integer pageSize);
    Map<String, Object> getUserBalance(Long userId);

}

