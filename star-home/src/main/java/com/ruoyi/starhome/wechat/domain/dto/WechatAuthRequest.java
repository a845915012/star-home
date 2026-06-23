package com.ruoyi.starhome.wechat.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信授权请求参数
 *
 * @author starhome
 */
@Data
@Schema(description = "微信授权请求")
public class WechatAuthRequest {

    @NotBlank(message = "code 不能为空")
    @Schema(description = "微信登录/授权临时 code", required = true, example = "081abcDEF123xyz")
    private String code;
}
