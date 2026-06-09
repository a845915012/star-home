package com.ruoyi.starhome.sms.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 阿里云短信发送状态报告(回执)
 * <p>
 * 阿里云会通过 HTTP/HTTPS 回调将短信的最终投递状态以 JSON 数组形式推送到回执接口。
 * 字段定义参考阿里云官方文档「短信发送状态报告」。
 * </p>
 */
@Data
@Schema(description = "阿里云短信发送状态报告(回执)")
public class SmsReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接收短信的手机号码
     */
    @JsonProperty("phone_number")
    @Schema(description = "接收短信的手机号码")
    private String phoneNumber;

    /**
     * 短信发送时间
     */
    @JsonProperty("send_time")
    @Schema(description = "短信发送时间")
    private String sendTime;

    /**
     * 状态报告时间
     */
    @JsonProperty("report_time")
    @Schema(description = "状态报告时间")
    private String reportTime;

    /**
     * 是否接收成功
     */
    @JsonProperty("success")
    @Schema(description = "是否接收成功")
    private Boolean success;

    /**
     * 状态码: DELIVERED 表示发送成功，其他为失败错误码
     */
    @JsonProperty("err_code")
    @Schema(description = "状态码,DELIVERED表示发送成功")
    private String errCode;

    /**
     * 状态码的描述
     */
    @JsonProperty("err_msg")
    @Schema(description = "状态码的描述")
    private String errMsg;

    /**
     * 计费条数
     */
    @JsonProperty("sms_size")
    @Schema(description = "计费条数")
    private String smsSize;

    /**
     * 发送短信时返回的流水号(SendResponse 中的 BizId)
     */
    @JsonProperty("biz_id")
    @Schema(description = "发送短信流水号BizId")
    private String bizId;

    /**
     * 发送短信时透传的外部流水扩展字段
     */
    @JsonProperty("out_id")
    @Schema(description = "外部流水扩展字段OutId")
    private String outId;
}
