package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * 视频任务分页单条数据
 */
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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getSeconds() {
        return seconds;
    }

    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrlRemote() {
        return videoUrlRemote;
    }

    public void setVideoUrlRemote(String videoUrlRemote) {
        this.videoUrlRemote = videoUrlRemote;
    }

    public Integer getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Integer isComplete) {
        this.isComplete = isComplete;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }
}
