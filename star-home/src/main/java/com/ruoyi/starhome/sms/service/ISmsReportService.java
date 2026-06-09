package com.ruoyi.starhome.sms.service;

import com.ruoyi.starhome.sms.domain.SmsReport;

import java.util.List;

/**
 * 阿里云短信回执处理 Service
 */
public interface ISmsReportService {

    /**
     * 处理阿里云推送的短信状态报告(回执)
     *
     * @param reports 阿里云回调推送的状态报告列表
     */
    void handleReports(List<SmsReport> reports);
}
