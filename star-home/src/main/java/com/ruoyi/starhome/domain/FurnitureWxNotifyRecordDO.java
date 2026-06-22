package com.ruoyi.starhome.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信通知记录实体
 */
@Data
@TableName("furniture_wx_notify_record")
public class FurnitureWxNotifyRecordDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 接收者微信openid */
    private String openid;

    /** 通知类型：VIDEO_SUCCESS-视频生成成功, VIDEO_FAIL-视频生成失败, RECHARGE_SUCCESS-充值成功 */
    private String notifyType;

    /** 微信模板消息ID */
    private String templateId;

    /** 通知内容（JSON格式） */
    private String content;

    /** 发送状态：0-待发送, 1-发送成功, 2-发送失败 */
    private Integer sendStatus;

    /** 实际发送时间 */
    private LocalDateTime sendTime;

    /** 发送失败时的错误信息 */
    private String errorMsg;

    /** 业务ID（如generationTaskId、orderNo等） */
    private String bizId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
