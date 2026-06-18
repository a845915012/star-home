package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信 JSSDK 签名响应
 */
@Data
@Schema(description = "微信 JSSDK 签名响应")
public class WechatJsSdkSignatureResponse {

    @Schema(description = "微信应用ID", example = "wx1234567890abcdef")
    private String appId;

    @Schema(description = "页面URL", example = "https://xinglianjia.cn/studio/index.html")
    private String url;

    @Schema(description = "时间戳", example = "1718697600")
    private String timestamp;

    @Schema(description = "随机串", example = "c0d4bd66c89b4f9d9f61b6457f1c9952")
    private String nonceStr;

    @Schema(description = "签名", example = "9e842ad396f5a54e9139c1ffcc31f22d3372cc8d")
    private String signature;
}
