package com.ruoyi.starhome.enums;

/**
 * apiNumber枚举常量
 */
public enum ApiNumberConstants {
    IMAGE2IMAGE_YUNWU("image2image_yunwu_api"),
    LINGGANWENAN_YUNWU("lingganwenan_yunwu"),
    LINGGANWENAN_STREAM_YUNWU("lingganwenan_stream_yunwu"),
    IMAGE2VIDEO_YUNWU("image2video_yunwu_api");

    private final String apiNumber;
    ApiNumberConstants(String apiNumber) {
        this.apiNumber = apiNumber;
    }
    // 提供 getter 方法获取 BigDecimal 值
    public String getApiNumber() {
        return apiNumber;
    }
}
