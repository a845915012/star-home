-- 充值订单表
-- 在运行项目之前，请先在数据库中执行此SQL创建表

CREATE TABLE IF NOT EXISTS `furniture_recharge_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` varchar(64) NOT NULL COMMENT '充值订单号',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `amount` decimal(10,2) NOT NULL COMMENT '充值金额(元)',
    `pay_status` int NOT NULL DEFAULT 0 COMMENT '支付状态: 0-待支付, 1-支付成功, 2-支付失败, 3-已关闭',
    `pay_way` varchar(32) DEFAULT 'alipay' COMMENT '支付方式: alipay-支付宝, wechat-微信',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `transaction_id` varchar(128) DEFAULT NULL COMMENT '第三方交易流水号',
    `subject` varchar(256) DEFAULT NULL COMMENT '订单标题',
    `body` varchar(512) DEFAULT NULL COMMENT '订单描述',
    `notify_time` datetime DEFAULT NULL COMMENT '回调通知时间',
    `notify_content` text COMMENT '回调通知内容(JSON)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_pay_status` (`pay_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';

ALTER TABLE `furniture_recharge_package` ADD COLUMN `vip_day` int DEFAULT NULL COMMENT '会员天数';
