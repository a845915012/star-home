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

@Data
@TableName("furniture_design_task")
public class FurnitureDesignTaskDO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String originalImageUrl;
    private String styleType;
    private String perspective;
    private Integer status;
    private String resultImageUrl;
    private BigDecimal apiCost;
    private Integer apiDuration;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Date completeTime;
}
