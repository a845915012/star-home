package com.ruoyi.starhome.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 充值订单实体
 */
@Data
@TableName("furniture_recharge_order")
public class FurnitureRechargeOrderDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 充值订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 充值金额(元)
     */
    private BigDecimal amount;

    /**
     * 充值套餐ID
     */
    private Long packageId;

    /**
     * 到账金额(元)
     */
    private BigDecimal provideAmount;

    /**
     * 支付状态: 0-待支付, 1-支付成功, 2-支付失败, 3-已关闭
     */
    private Integer payStatus;

    /**
     * 支付方式: alipay-支付宝, wechat-微信
     */
    private String payWay;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 第三方交易流水号
     */
    private String transactionId;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 订单描述
     */
    private String body;

    /**
     * 回调通知时间
     */
    private Date notifyTime;

    /**
     * 回调通知内容(JSON)
     */
    private String notifyContent;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;
}
