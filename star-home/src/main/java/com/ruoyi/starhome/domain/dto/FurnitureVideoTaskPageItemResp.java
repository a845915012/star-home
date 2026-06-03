package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 视频任务分页单条数据
 */
@Data
public class FurnitureVideoTaskPageItemResp {

    @Schema(description = "尺寸")
    private String size;

    @Schema(description = "秒数")
    private Integer seconds;

    @Schema(description = "进度")
    private String progress;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "请求时的描述")
    private String prompt;

    @Schema(description = "请求时的图片地址")
    private String imageUrl;

    @Schema(description = "外部视频地址")
    private String videoUrlRemote;

    @Schema(description = "任务完成状态(0:未完成,1:已完成)")
    private Integer isComplete;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "完成时间")
    private Date finishTime;

}
