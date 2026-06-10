-- 消费配置表
-- 在运行项目之前，请先在数据库中执行此 SQL 创建表并初始化数据

CREATE TABLE IF NOT EXISTS `furniture_consume_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `consume_code` varchar(64) NOT NULL COMMENT '消费项编码',
    `consume_name` varchar(100) NOT NULL COMMENT '消费项名称',
    `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '消费金额',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_consume_code` (`consume_code`),
    KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费配置表';

INSERT INTO `furniture_consume_config` (`consume_code`, `consume_name`, `price`, `status`, `sort`, `remark`)
VALUES
    ('TEST', '测试调用', 0.00, 1, 0, '测试用途'),
    ('IMAGE2TEXT', '图片生成文案', 5.00, 1, 10, '原 ConsumeConstants.IMAGE2TEXT'),
    ('IMAGE2VIDEOTEXT', '图片生成视频文案', 1.00, 1, 20, '原 ConsumeConstants.IMAGE2VIDEOTEXT'),
    ('IMAGE2IMAGE_DRAFT', '图片生成草稿图', 2.00, 1, 30, '原 ConsumeConstants.IMAGE2IMAGE_DRAFT'),
    ('IMAGE2IMAGE_FINAL', '图片生成成品图', 9.90, 1, 40, '原 ConsumeConstants.IMAGE2IMAGE_FINAL'),
    ('IMAGE2VIDEO', '图片生成视频', 19.90, 1, 50, '原 ConsumeConstants.IMAGE2VIDEO')
ON DUPLICATE KEY UPDATE
    `consume_name` = VALUES(`consume_name`),
    `price` = VALUES(`price`),
    `status` = VALUES(`status`),
    `sort` = VALUES(`sort`),
    `remark` = VALUES(`remark`);

-- The video generation flow charges after async merge succeeds, so persist
-- the selected consume config on the generation header.
ALTER TABLE `furniture_video_generation_task`
    ADD COLUMN `consume_code` varchar(64) DEFAULT NULL COMMENT '消费项编码' AFTER `image_url`,
    ADD COLUMN `consume_price` decimal(10,2) DEFAULT NULL COMMENT '消费金额' AFTER `consume_code`;
