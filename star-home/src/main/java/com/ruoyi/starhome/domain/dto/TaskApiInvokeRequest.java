package com.ruoyi.starhome.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TaskApiInvokeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Integer apiNumber;
}
