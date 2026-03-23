package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private Long id;
    @Schema(description = "电话", example = "1")
    private String phone;
    @Schema(description = "邮箱", example = "1")
    private String email;
    @Schema(description = "状态", example = "1")
    private Integer status;
    @Schema(description = "密码", example = "1")
    private String password;
}
