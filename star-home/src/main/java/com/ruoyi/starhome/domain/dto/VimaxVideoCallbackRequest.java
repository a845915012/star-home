package com.ruoyi.starhome.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * vimax-agent 视频任务回调请求体
 */
@Data
public class VimaxVideoCallbackRequest {

    /**
     * 任务ID，对应创建任务时返回的 job_id
     */
    @JsonProperty("job_id")
    private String jobId;

    /**
     * 任务状态：completed / failed
     */
    private String status;

    /**
     * 任务进度描述
     */
    private String progress;

    /**
     * 失败原因
     */
    private String error;

    /**
     * 视频结果 URL
     */
    @JsonProperty("result_url")
    private String resultUrl;

    /**
     * vimax-agent 生成的 prompt
     */
    private String prompt;

    /**
     * 任务完成时间 (ISO 8601)
     */
    @JsonProperty("finished_at")
    private String finishedAt;
}
