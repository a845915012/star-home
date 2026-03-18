package com.ruoyi.starhome.domain.dto;

import com.ruoyi.starhome.domain.FurnitureUserBalanceRecordsDO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FurnitureUserBalanceRecordsPageResp {
    private String username;
    private BigDecimal balance;
    private BigDecimal useBalance;
    private List<FurnitureUserBalanceRecordsDO> list;
}
