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

    /**
     * 原子条件扣费：仅当余额 >= amount 时扣除，返回是否扣费成功。
     * 用于并发场景，避免"先查后扣"导致的少扣/超扣。
     */
    boolean deductIfEnough(Long userId, BigDecimal amount);

    /**
     * 退款（生成失败时回退扣费）。
     */
    void refund(Long userId, BigDecimal amount);

    /**
     * 仅写入消费流水（余额已在 deductIfEnough 中扣除）。
     */
    void recordConsume(Long userId, BigDecimal amount);

    FurnitureUserBalanceRecordsPageResp getUserBalanceRecords(Long userId, Integer type, Integer pageNum, Integer pageSize);
    Map<String, Object> getUserBalance(Long userId);

}

