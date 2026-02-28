package com.ruoyi.system.domain.starhome;

import com.ruoyi.common.core.domain.BaseEntity;

import java.util.Date;

public class FurnitureApiCallMonitor extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Integer minuteCalls;
    private Integer hourCalls;
    private Date lastCallTime;
    private Boolean isBlocked;
    private Date blockTime;
    private String blockReason;
    private Date unblockTime;
    private Date minuteWindowStart;
    private Date hourWindowStart;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getMinuteCalls() { return minuteCalls; }
    public void setMinuteCalls(Integer minuteCalls) { this.minuteCalls = minuteCalls; }
    public Integer getHourCalls() { return hourCalls; }
    public void setHourCalls(Integer hourCalls) { this.hourCalls = hourCalls; }
    public Date getLastCallTime() { return lastCallTime; }
    public void setLastCallTime(Date lastCallTime) { this.lastCallTime = lastCallTime; }
    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }
    public Date getBlockTime() { return blockTime; }
    public void setBlockTime(Date blockTime) { this.blockTime = blockTime; }
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    public Date getUnblockTime() { return unblockTime; }
    public void setUnblockTime(Date unblockTime) { this.unblockTime = unblockTime; }
    public Date getMinuteWindowStart() { return minuteWindowStart; }
    public void setMinuteWindowStart(Date minuteWindowStart) { this.minuteWindowStart = minuteWindowStart; }
    public Date getHourWindowStart() { return hourWindowStart; }
    public void setHourWindowStart(Date hourWindowStart) { this.hourWindowStart = hourWindowStart; }
}
