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
 * API费用明细统计对象 furniture_api_cost_stat
 */
@Data
@TableName("furniture_api_cost_stat")
public class FurnitureApiCostStatDO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Date statDate;
    private Integer totalCalls;
    private Integer totalUsers;
    private BigDecimal apiCost;
    private BigDecimal revenue;
    private BigDecimal grossProfit;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
