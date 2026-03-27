package com.ruoyi.starhome.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("furniture_video_task")
public class FurnitureVideoTaskDO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskId;
    private String model;
    private String progress;
    private String status;
    private BigDecimal cost;
    private String size;
    private Integer seconds;
    private String failReason;
    private String prompt;
    private String imageUrl;
    private String videoUrlRemote;
    private String videoUrlLocal;
    private Integer isComplete;
    private Date startTime;
    private Date finishTime;
}
