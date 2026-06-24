package com.ruoyi.starhome.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单 VO（含用户信息）
 */
@Data
public class FurnitureOrderVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long userId;
    private Long packageId;
    private BigDecimal amount;
    private Integer payStatus;
    private String payWay;
    private Date payTime;
    private String transactionId;
    private Date createTime;
    private String remark;

    /** 用户账号 */
    private String userName;

    /** 手机号码 */
    private String phonenumber;
}
