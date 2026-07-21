package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SceneResultItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "入参序号，用于与请求列表顺序一一对应")
    private int index;

    @Schema(description = "是否成功生成")
    private boolean success;

    @Schema(description = "接口编码")
    private String apiNumber;

    @Schema(description = "生成结果（成功时返回图片地址）")
    private String apiResult;

    @Schema(description = "本次花费")
    private BigDecimal callCost;

    @Schema(description = "失败原因（成功时为空）")
    private String failReason;

    public SceneResultItem fail(String reason) {
        this.success = false;
        this.failReason = reason;
        return this;
    }
}
