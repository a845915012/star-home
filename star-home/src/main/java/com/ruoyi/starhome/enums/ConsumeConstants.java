package com.ruoyi.starhome.enums;

import java.math.BigDecimal;

public enum ConsumeConstants {
    TEST(BigDecimal.ZERO),
    IMAGE2TEXT(new BigDecimal("5.0")),
    IMAGE2VIDEOTEXT(new BigDecimal("1.0")),
    // 图生图草稿
    IMAGE2IMAGE_DRAFT(new BigDecimal("2.0")),
    // 图生图最终版
    IMAGE2IMAGE_FINAL(new BigDecimal("9.9")),
    // 图生视频
    IMAGE2VIDEO(new BigDecimal("19.9"));



    private final BigDecimal price;
    ConsumeConstants(BigDecimal price) {
        this.price = price;
    }
    // 提供 getter 方法获取 BigDecimal 值
    public BigDecimal getPrice() {
        return price;
    }
}
