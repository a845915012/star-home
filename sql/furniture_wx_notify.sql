-- =============================================
-- 微信通知功能相关表结构变更
-- =============================================

-- 1. sys_user 表增加微信 openid 字段
-- 目的：关联微信用户身份，用于向用户发送公众号模板消息通知
ALTER TABLE `sys_user`
    ADD COLUMN `wx_openid` varchar(128) DEFAULT NULL COMMENT '微信公众号openid' AFTER `vip_expire_time`;

-- 2. 微信通知记录表
-- 目的：记录每次向用户发送的微信模板消息通知，便于追踪通知发送状态和排查问题
DROP TABLE IF EXISTS `furniture_wx_notify_record`;
CREATE TABLE `furniture_wx_notify_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID（关联sys_user.user_id）',
    `openid` varchar(128) NOT NULL COMMENT '接收者微信openid',
    `notify_type` varchar(64) NOT NULL COMMENT '通知类型：VIDEO_SUCCESS-视频生成成功, VIDEO_FAIL-视频生成失败, RECHARGE_SUCCESS-充值成功',
    `template_id` varchar(128) DEFAULT NULL COMMENT '微信模板消息ID',
    `content` text COMMENT '通知内容（JSON格式，记录模板消息实际发送数据）',
    `send_status` tinyint NOT NULL DEFAULT 0 COMMENT '发送状态：0-待发送, 1-发送成功, 2-发送失败',
    `send_time` datetime DEFAULT NULL COMMENT '实际发送时间',
    `error_msg` varchar(1024) DEFAULT NULL COMMENT '发送失败时的错误信息',
    `biz_id` varchar(128) DEFAULT NULL COMMENT '业务ID（如generationTaskId、orderNo等）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_openid` (`openid`),
    KEY `idx_notify_type` (`notify_type`),
    KEY `idx_send_status` (`send_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信通知记录表';
