package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.starhome.domain.dto.WechatJsSdkSignatureResponse;
import com.ruoyi.starhome.service.IWechatPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "微信JSSDK")
@RestController
@RequestMapping("/starhome/wechat/jssdk")
public class WechatJsSdkController {

    @Autowired
    private IWechatPayService wechatPayService;

    @Operation(summary = "生成微信JSSDK签名", description = "根据前端当前页面URL生成微信JSSDK签名参数")
    @Parameter(name = "url", description = "当前页面完整URL，需和前端实际访问地址一致且不带#号后内容", example = "https://xinglianjia.cn/studio/index.html", required = true)
    @GetMapping("/signature")
    public R<WechatJsSdkSignatureResponse> signature(@RequestParam("url") String url) {
        WechatJsSdkSignatureResponse response = wechatPayService.getJsSdkSignature(url);
        return R.ok(response);
    }
}
