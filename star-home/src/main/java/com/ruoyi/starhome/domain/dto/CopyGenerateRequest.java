package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
@Data
public class CopyGenerateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "编码")
    private String apiNumber;
    @Schema(description = "原始图片URL数组")
    private List<String> filePaths;
    @Schema(description = "风格提示词")
    private String stylePrompt;
    @Schema(description = "用户自定义提示词")
    private String userPrompt;
}
