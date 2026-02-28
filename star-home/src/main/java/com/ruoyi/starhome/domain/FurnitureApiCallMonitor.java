package com.ruoyi.starhome.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 家居接口调用监控对象 furniture_api_call_monitor
 *
 * @author ruoyi
 */
@Data
public class FurnitureApiCallMonitor implements Serializable {
    @Serial
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
}
