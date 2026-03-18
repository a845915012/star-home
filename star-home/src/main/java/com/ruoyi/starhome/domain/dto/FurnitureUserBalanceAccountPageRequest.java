package com.ruoyi.starhome.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户钱包分页查询请求
 */
public class FurnitureUserBalanceAccountPageRequest extends BasePageRequest {
    @Schema(description = "用户名", example = "alice")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
