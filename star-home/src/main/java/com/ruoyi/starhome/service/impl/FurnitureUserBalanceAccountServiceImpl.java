package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.starhome.domain.FurnitureUserBalanceAccountDO;
import com.ruoyi.starhome.domain.FurnitureUserBalanceRecordsDO;
import com.ruoyi.starhome.domain.dto.FurnitureUserBalanceRecordsPageResp;
import com.ruoyi.starhome.domain.vo.FurnitureUserBalanceAccountPageVO;
import com.ruoyi.starhome.mapper.FurnitureUserBalanceAccountMapper;
import com.ruoyi.starhome.mapper.FurnitureUserBalanceRecordsMapper;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import com.ruoyi.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class FurnitureUserBalanceAccountServiceImpl implements IFurnitureUserBalanceAccountService {
    private static final int TYPE_RECHARGE = 1;
    private static final int TYPE_CONSUME = 2;

    @Autowired
    private FurnitureUserBalanceAccountMapper furnitureUserBalanceAccountMapper;

    @Autowired
    private FurnitureUserBalanceRecordsMapper furnitureUserBalanceRecordsMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public List<FurnitureUserBalanceAccountPageVO> selectFurnitureUserBalanceAccountList(String username) {
        return furnitureUserBalanceAccountMapper.selectFurnitureUserBalanceAccountList(username);
    }

    @Override
    public FurnitureUserBalanceAccountDO selectFurnitureUserBalanceAccountByUserId(Long userId) {
        return furnitureUserBalanceAccountMapper.selectFurnitureUserBalanceAccountByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        validateOperate(userId, amount);
        FurnitureUserBalanceAccountDO account = ensureAccount(userId);
        BigDecimal balance = safeAmount(account.getBalance()).add(amount);
        account.setBalance(balance);
        account.setUpdateTime(new Date());
        furnitureUserBalanceAccountMapper.updateById(account);
        insertRecord(userId, amount, TYPE_RECHARGE, "充值");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(Long userId, BigDecimal amount) {
        validateOperate(userId, amount);
        FurnitureUserBalanceAccountDO account = ensureAccount(userId);
        BigDecimal balance = safeAmount(account.getBalance());
        if (balance.compareTo(amount) < 0) {
            throw new ServiceException("余额不足");
        }
        account.setBalance(balance.subtract(amount));
        account.setUseBalance(safeAmount(account.getUseBalance()).add(amount));
        account.setUpdateTime(new Date());
        furnitureUserBalanceAccountMapper.updateById(account);
        insertRecord(userId, amount, TYPE_CONSUME, "消费");
    }

    @Override
    public FurnitureUserBalanceRecordsPageResp getUserBalanceRecords(Long userId) {
        if (userId == null) {
            throw new ServiceException("userId不能为空");
        }
        FurnitureUserBalanceAccountPageVO summary = furnitureUserBalanceAccountMapper.selectUserBalanceSummaryByUserId(userId);
        FurnitureUserBalanceRecordsPageResp resp = new FurnitureUserBalanceRecordsPageResp();
        if (summary != null) {
            resp.setUsername(summary.getUsername());
            resp.setBalance(summary.getBalance());
            resp.setUseBalance(summary.getUseBalance());
        } else {
            SysUser user = sysUserMapper.selectUserById(userId);
            if (user != null) {
                resp.setUsername(user.getUserName());
            }
            resp.setBalance(BigDecimal.ZERO);
            resp.setUseBalance(BigDecimal.ZERO);
        }
        List<FurnitureUserBalanceRecordsDO> records = furnitureUserBalanceRecordsMapper.selectList(
                new LambdaQueryWrapper<FurnitureUserBalanceRecordsDO>()
                        .eq(FurnitureUserBalanceRecordsDO::getUserId, userId)
                        .orderByDesc(FurnitureUserBalanceRecordsDO::getId)
        );
        resp.setList(records);
        return resp;
    }

    private void validateOperate(Long userId, BigDecimal amount) {
        if (userId == null) {
            throw new ServiceException("userId不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("amount必须大于0");
        }
    }

    private FurnitureUserBalanceAccountDO ensureAccount(Long userId) {
        FurnitureUserBalanceAccountDO account = furnitureUserBalanceAccountMapper.selectFurnitureUserBalanceAccountByUserId(userId);
        if (account == null) {
            account = new FurnitureUserBalanceAccountDO();
            account.setUserId(userId);
            account.setBalance(BigDecimal.ZERO);
            account.setUseBalance(BigDecimal.ZERO);
            account.setCreateTime(new Date());
            account.setUpdateTime(new Date());
            furnitureUserBalanceAccountMapper.insert(account);
        }
        return account;
    }

    private void insertRecord(Long userId, BigDecimal amount, int type, String remark) {
        FurnitureUserBalanceRecordsDO record = new FurnitureUserBalanceRecordsDO();
        record.setUserId(userId);
        record.setType(type);
        record.setAmount(amount);
        record.setRemark(remark);
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        furnitureUserBalanceRecordsMapper.insert(record);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
