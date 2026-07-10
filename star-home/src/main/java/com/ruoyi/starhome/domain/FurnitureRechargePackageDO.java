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
import java.time.LocalDateTime;

@Data
@TableName("furniture_recharge_package")
public class FurnitureRechargePackageDO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String packageName;

    private BigDecimal costAmount;

    private BigDecimal provideAmount;

    private Integer isVip;

    private Integer vipDay;

    /** 会员等级（1-普通会员 2-高级会员等） */
    private Integer vipLevel;

    private String status;

    private String remark;

    /** 活动开始时间 */
    private LocalDateTime activityStartTime;

    /** 活动结束时间 */
    private LocalDateTime activityEndTime;

    /** 总限量，-1为无限 */
    private Integer totalQuota;

    /** 每日限量，-1为无限 */
    private Integer dailyQuota;

    /** 是否过期，0：过期，1：未过期（由定时任务根据活动时间维护） */
    private Integer isExpire;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
