package com.ruoyi.starhome.sms.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.starhome.sms.domain.SmsReport;
import com.ruoyi.starhome.sms.service.ISmsReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云短信回执(状态报告)回调控制器
 * <p>
 * 在阿里云短信控制台配置「短信发送状态报告」HTTP/HTTPS 推送地址时,
 * 填写本接口的公网 HTTPS 地址,例如:
 * https://your-domain.com/starhome/sms/report
 * </p>
 * <p>
 * 阿里云以 JSON 数组形式 POST 推送状态报告,接口需公网可访问且免登录。
 * 接收成功后必须返回固定 JSON {"code":0,"msg":"成功"},否则阿里云会重复推送。
 * </p>
 */
@Tag(name = "阿里云短信回执")
@RestController
@RequestMapping("/starhome/sms")
public class SmsReportController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(SmsReportController.class);

    @Autowired
    private ISmsReportService smsReportService;

    /**
     * 接收阿里云短信发送状态报告(回执)
     * <p>
     * 注意: 该接口必须配置为公网可访问的 HTTPS 地址,且不需要登录验证。
     * </p>
     *
     * @param reports 阿里云推送的状态报告列表
     * @return 固定应答,告知阿里云已成功接收
     */
    @Anonymous
    @Operation(summary = "短信状态报告回执", description = "接收阿里云通过HTTPS推送的短信投递状态报告,需配置为公网可访问地址")
    @PostMapping("/report")
    public Map<String, Object> report(@RequestBody List<SmsReport> reports) {
        log.info("收到阿里云短信状态报告回执, 数量: {}", reports == null ? 0 : reports.size());

        smsReportService.handleReports(reports);

        // 阿里云要求接收成功后返回固定应答,否则会按重试策略重复推送
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "成功");
        return result;
    }
}
