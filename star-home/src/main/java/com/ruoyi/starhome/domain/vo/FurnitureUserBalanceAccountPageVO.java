package com.ruoyi.starhome.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
@Data
public class FurnitureUserBalanceAccountPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private BigDecimal balance;

    private BigDecimal useBalance;

    private Date createTime;

    private Date updateTime;

    private String username;
}
