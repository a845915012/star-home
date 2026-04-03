package com.ruoyi.starhome.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("furniture_video_generation_task")
public class FurnitureVideoGenerationTaskDO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    /**
     * 产品主体
     */
    private String product;
    /**
     * 材质
     */
    private String material;
    private String imageUrl;
    /**
     * 期望任务数
     */
    private Integer expectedTaskCount;
    /**
     * 当前任务数
     */
    private Integer currentTaskCount;
    /**
     * 状态
     */
    private String status;
    /**
     * 最终远程视频地址
     */
    private String remoteFinalVideoUrl;
    /**
     * 最终本地视频地址
     */
    private String localFinalVideoUrl;
    /**
     * 错误信息
     */
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
