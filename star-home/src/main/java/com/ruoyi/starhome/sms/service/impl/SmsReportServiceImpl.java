package com.ruoyi.starhome.sms.service.impl;

import com.ruoyi.starhome.sms.domain.SmsReport;
import com.ruoyi.starhome.sms.service.ISmsReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 阿里云短信回执处理 Service 实现
 * <p>
 * 阿里云投递状态报告中,err_code 为 "DELIVERED" 表示短信成功送达终端;
 * 其余错误码表示发送失败,可在此处根据业务需要落库或更新发送记录状态。
 * </p>
 */
@Service
public class SmsReportServiceImpl implements ISmsReportService {

    private static final Logger log = LoggerFactory.getLogger(SmsReportServiceImpl.class);

    /**
     * 投递成功的状态码
     */
    private static final String CODE_DELIVERED = "DELIVERED";

    @Override
    public void handleReports(List<SmsReport> reports) {
        if (CollectionUtils.isEmpty(reports)) {
            log.warn("收到空的短信状态报告回执");
            return;
        }

        for (SmsReport report : reports) {
            boolean delivered = CODE_DELIVERED.equalsIgnoreCase(report.getErrCode());
            if (delivered) {
                log.info("短信送达成功, phone={}, bizId={}, outId={}, reportTime={}",
                        report.getPhoneNumber(), report.getBizId(), report.getOutId(), report.getReportTime());
            } else {
                log.warn("短信送达失败, phone={}, bizId={}, errCode={}, errMsg={}, reportTime={}",
                        report.getPhoneNumber(), report.getBizId(), report.getErrCode(),
                        report.getErrMsg(), report.getReportTime());
            }

            // TODO: 根据业务需要,可在此处理回执:
            //  1. 根据 bizId / outId 找到对应的短信发送记录;
            //  2. 更新记录的最终投递状态(成功/失败)、计费条数、状态报告时间等;
            //  3. 失败时按业务策略进行补发或告警。
        }
    }
}
