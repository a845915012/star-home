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
     * 充值成功通知（可选）
     *
     * @param userId  用户ID
     * @param orderNo 充值订单号
     * @param amount  充值金额
     */
    void notifyRechargeSuccess(Long userId, String orderNo, String amount);
}
