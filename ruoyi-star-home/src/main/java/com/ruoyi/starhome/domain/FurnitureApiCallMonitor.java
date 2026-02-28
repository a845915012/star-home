package com.ruoyi.starhome.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 家居接口调用监控对象 furniture_api_call_monitor
 *
 * @author ruoyi
 */
public class FurnitureApiCallMonitor extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 分钟窗口调用次数 */
    private Integer minuteCalls;

    /** 小时窗口调用次数 */
    private Integer hourCalls;

    /** 最近调用时间 */
    private Date lastCallTime;

    /** 是否封禁 */
    private Boolean isBlocked;

    /** 封禁时间 */
    private Date blockTime;

    /** 封禁原因 */
    private String blockReason;

    /** 解封时间 */
    private Date unblockTime;

    /** 分钟窗口开始时间 */
    private Date minuteWindowStart;

    /** 小时窗口开始时间 */
    private Date hourWindowStart;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getMinuteCalls() {
        return minuteCalls;
    }

    public void setMinuteCalls(Integer minuteCalls) {
        this.minuteCalls = minuteCalls;
    }

    public Integer getHourCalls() {
        return hourCalls;
    }

    public void setHourCalls(Integer hourCalls) {
        this.hourCalls = hourCalls;
    }

    public Date getLastCallTime() {
        return lastCallTime;
    }

    public void setLastCallTime(Date lastCallTime) {
        this.lastCallTime = lastCallTime;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Boolean blocked) {
        isBlocked = blocked;
    }

    public Date getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(Date blockTime) {
        this.blockTime = blockTime;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public Date getUnblockTime() {
        return unblockTime;
    }

    public void setUnblockTime(Date unblockTime) {
        this.unblockTime = unblockTime;
    }

    public Date getMinuteWindowStart() {
        return minuteWindowStart;
    }

    public void setMinuteWindowStart(Date minuteWindowStart) {
        this.minuteWindowStart = minuteWindowStart;
    }

    public Date getHourWindowStart() {
        return hourWindowStart;
    }

    public void setHourWindowStart(Date hourWindowStart) {
        this.hourWindowStart = hourWindowStart;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("minuteCalls", getMinuteCalls())
                .append("hourCalls", getHourCalls())
                .append("lastCallTime", getLastCallTime())
                .append("isBlocked", getIsBlocked())
                .append("blockTime", getBlockTime())
                .append("blockReason", getBlockReason())
                .append("unblockTime", getUnblockTime())
                .append("minuteWindowStart", getMinuteWindowStart())
                .append("hourWindowStart", getHourWindowStart())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
