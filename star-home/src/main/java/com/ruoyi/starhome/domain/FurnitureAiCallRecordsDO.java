package com.ruoyi.starhome.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AI调用记录对象 furniture_ai_call_records
 */
@Data
@TableName("furniture_ai_call_records")
public class FurnitureAiCallRecordsDO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户id */
    private Long userId;

    /** 模块 */
    private String module;

    /** ai模型 */
    private String aiMode;

    /** 输入Token */
    private BigDecimal tokenIn;

    /** 输出Token */
    private BigDecimal tokenOut;

    /** 总Token */
    private BigDecimal totalToken;

    /** 费用 */
    private BigDecimal cost;

    /** 创建时间 */
    private Date createTime;
}
