package com.ruoyi.starhome.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * AI调用记录 VO（含用户信息）
 */
@Data
public class FurnitureAiCallRecordsVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String module;
    private String aiMode;
    private BigDecimal tokenIn;
    private BigDecimal tokenOut;
    private BigDecimal totalToken;
    private BigDecimal cost;
    private Long generationTaskId;
    private String prompt;
    private String userPrompt;
    private String inputFiles;
    private String outputFiles;
    private String ouputContent;
    private String status;
    private Date createTime;

    /** 用户账号 */
    private String userName;

    /** 手机号码 */
    private String phonenumber;
}
