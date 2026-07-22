package com.ruoyi.starhome.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.starhome.domain.FurnitureUserBalanceAccountDO;
import com.ruoyi.starhome.domain.FurnitureUserBalanceRecordsDO;
import com.ruoyi.starhome.domain.dto.FurnitureUserBalanceRecordsPageResp;
import com.ruoyi.starhome.domain.vo.FurnitureUserBalanceAccountPageVO;
import com.ruoyi.starhome.mapper.FurnitureUserBalanceAccountMapper;
import com.ruoyi.starhome.mapper.FurnitureUserBalanceRecordsMapper;
import com.ruoyi.starhome.service.IFurnitureUserBalanceAccountService;
import com.ruoyi.starhome.utils.DateUtil;
import com.ruoyi.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FurnitureUserBalanceAccountServiceImpl implements IFurnitureUserBalanceAccountService {
    private static final int TYPE_RECHARGE = 1;
    private static final int TYPE_CONSUME = 2;
    private static final int TYPE_REFUND = 3;
    private static final BigDecimal NEW_USER_INIT_BALANCE = new BigDecimal("10");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern(DateUtil.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND);

    @Autowired
    private FurnitureUserBalanceAccountMapper furnitureUserBalanceAccountMapper;

    @Autowired
    private FurnitureUserBalanceRecordsMapper furnitureUserBalanceRecordsMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserRegistered(SysRegisterService.UserRegisteredEvent event) {
        if (event == null || event.getUserId() == null) {
            throw new ServiceException("用户注册事件缺少userId");
        }
        initBalanceOnRegister(event.getUserId());
    }

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
    public void recharge(Long userId, BigDecimal amount, String payWay) {
        validateOperate(userId, amount);
        FurnitureUserBalanceAccountDO account = ensureAccount(userId);
        BigDecimal balance = safeAmount(account.getBalance()).add(amount);
        account.setBalance(balance);
        account.setUpdateTime(new Date());
        furnitureUserBalanceAccountMapper.updateById(account);

        String remark = buildRechargeRemark(amount, payWay);
        insertRecord(userId, amount, TYPE_RECHARGE, remark);
    }

    /**
     * 构建充值备注：微信充值到账X星币 / 支付宝充值到账X星币
     */
    private String buildRechargeRemark(BigDecimal amount, String payWay) {
        String payWayName;
        if ("alipay".equalsIgnoreCase(payWay)) {
            payWayName = "支付宝充值";
        } else if ("wechat".equalsIgnoreCase(payWay)) {
            payWayName = "微信充值";
        } else {
            payWayName = "充值";
        }
        return payWayName + "到账" + amount.stripTrailingZeros().toPlainString() + "星币";
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
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean deductIfEnough(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        validateOperate(userId, amount);
        ensureAccount(userId);
        return furnitureUserBalanceAccountMapper.deductIfEnough(userId, amount) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void refund(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        validateOperate(userId, amount);
        ensureAccount(userId);
        furnitureUserBalanceAccountMapper.refund(userId, amount);
        insertRecord(userId, amount, TYPE_REFUND, "生成失败退款");
    }

    @Override
    public void recordConsume(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        insertRecord(userId, amount, TYPE_CONSUME, "消费");
    }

    @Override
    public FurnitureUserBalanceRecordsPageResp getUserBalanceRecords(Long userId, Integer type, Integer pageNum, Integer pageSize) {
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

        int resolvedPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        PageHelper.startPage(resolvedPageNum, resolvedPageSize);
        List<FurnitureUserBalanceRecordsDO> records = furnitureUserBalanceRecordsMapper.selectList(
                new LambdaQueryWrapper<FurnitureUserBalanceRecordsDO>()
                        .eq(type != null, FurnitureUserBalanceRecordsDO::getType, type)
                        .eq(FurnitureUserBalanceRecordsDO::getUserId, userId)
                        .orderByDesc(FurnitureUserBalanceRecordsDO::getId)
        );
        PageInfo<FurnitureUserBalanceRecordsDO> pageInfo = new PageInfo<>(records);
        resp.setList(records);
        resp.setTotal(pageInfo.getTotal());
        resp.setPageNum(pageInfo.getPageNum());
        resp.setPageSize(pageInfo.getPageSize());
        resp.setPages(pageInfo.getPages());
        return resp;
    }

    @Override
    public Map<String, Object> getUserBalance(Long userId) {
        if (userId == null) {
            throw new ServiceException("userId不能为空");
        }
        FurnitureUserBalanceAccountDO furnitureUserBalanceRecordsDO = furnitureUserBalanceAccountMapper.selectOne(new LambdaQueryWrapper<FurnitureUserBalanceAccountDO>()
                .eq(FurnitureUserBalanceAccountDO::getUserId, userId));
        if (furnitureUserBalanceRecordsDO == null) {
            throw new ServiceException("用户账户不存在");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("balance", furnitureUserBalanceRecordsDO.getBalance());

        SysUser user = sysUserMapper.selectUserById(userId);
        Integer isVip = user != null && user.getIsVip() != null ? user.getIsVip() : 0;
        resp.put("isVip", isVip);

        LocalDateTime vipBeginTime = user != null ? user.getVipBeginTime() : null;
        LocalDateTime vipExpireTime = user != null ? user.getVipExpireTime() : null;
        resp.put("vipBeginTime", vipBeginTime != null ? vipBeginTime.format(DATE_TIME_FMT) : null);
        resp.put("vipExpireTime", vipExpireTime != null ? vipExpireTime.format(DATE_TIME_FMT) : null);
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

    private void initBalanceOnRegister(Long userId) {
        FurnitureUserBalanceAccountDO existing = furnitureUserBalanceAccountMapper.selectFurnitureUserBalanceAccountByUserId(userId);
        if (existing != null) {
            return;
        }
        FurnitureUserBalanceAccountDO account = new FurnitureUserBalanceAccountDO();
        account.setUserId(userId);
        account.setBalance(NEW_USER_INIT_BALANCE);
        account.setUseBalance(BigDecimal.ZERO);
        account.setCreateTime(new Date());
        account.setUpdateTime(new Date());
        furnitureUserBalanceAccountMapper.insert(account);
        insertRecord(userId, NEW_USER_INIT_BALANCE, TYPE_RECHARGE, "新用户注册");
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
