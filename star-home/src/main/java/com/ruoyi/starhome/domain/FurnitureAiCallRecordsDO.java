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

    /** 类型，1：视觉设计，2：灵感文案，3：动态影像 */
    private Integer type;

    /** 视频生成任务id */
    private Long generationTaskId;

    /** 提示词 */
    private String prompt;

    /** 输入文件url */
    private String inputFiles;

    /** 输出文件url */
    private String outputFiles;

    /** 输出内容 */
    private String ouputContent;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private Date createTime;
}
