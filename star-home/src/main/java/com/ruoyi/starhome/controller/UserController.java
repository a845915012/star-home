package com.ruoyi.starhome.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.starhome.domain.dto.UpdateUserRequest;
import com.ruoyi.starhome.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ruoyi.common.core.domain.AjaxResult.error;
import static com.ruoyi.common.core.domain.AjaxResult.success;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/starhome/user")
public class UserController {

    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private IUserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册")
    public AjaxResult register(@RequestBody RegisterBody user) {
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    @PostMapping("/updateUser")
    @Operation(summary = "修改用户信息", description = "修改用户信息")
    public AjaxResult updateUser(@RequestBody UpdateUserRequest request) {
        return success(userService.updateUser(request));
    }
}
