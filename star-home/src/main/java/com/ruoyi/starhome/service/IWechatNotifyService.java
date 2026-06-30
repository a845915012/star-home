package com.ruoyi.starhome.service;

/**
 * 微信模板消息通知服务
 */
public interface IWechatNotifyService {

    /**
     * 视频生成成功通知
     *
     * @param userId           用户ID
     * @param generationTaskId 生成任务ID
     * @param videoUrl         视频地址
     */
    void notifyVideoSuccess(Long userId, Long generationTaskId, String videoUrl);

    /**
     * 视频生成失败通知
     *
     * @param userId           用户ID
     * @param generationTaskId 生成任务ID
     * @param reason           失败原因
     */
    void notifyVideoFailed(Long userId, Long generationTaskId, String reason);

    /**
     * 充值成功通知
     *
     * @param userId        用户ID
     * @param orderNo       充值订单号
     * @param amount        支付金额（元）
     * @param payWay        支付方式（alipay / wechat）
     * @param coinAmount    到账星币数
     * @param coinBalance   星币余额
     */
    void notifyRechargeSuccess(Long userId, String orderNo, String amount,
                               String payWay, String coinAmount, String coinBalance);
    /**
     * 视频生成结果统一通知（已完成/失败/异常）
     *
     * @param userId           用户ID
     * @param generationTaskId 生成任务ID（工单编号）
     * @param taskName         工单名称
     * @param finishTime       结束时间
     * @param result           处理结果（已完成 / 失败 / 异常）
     */
    void notifyVideoResult(Long userId, Long generationTaskId, String taskName,
                           String finishTime, String result);
}
