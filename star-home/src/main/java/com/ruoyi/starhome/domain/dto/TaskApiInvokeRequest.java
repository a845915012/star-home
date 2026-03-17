package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class TaskApiInvokeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String apiNumber;
    private List<String> filePaths;
    private String question;
    /**
     * 是否使用SSE流式输出（与 /stream 接口配合）。
     * true: 走 streaming + 通过SseEmitter推送
     * false: 走 blocking + 直接HTTP返回
     */
    private Boolean useSse;
}
