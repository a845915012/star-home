package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.starhome.domain.dto.UpdateUserRequest;
import com.ruoyi.starhome.service.IUserService;
import com.ruoyi.starhome.sms.service.ISmsCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/starhome/user")
public class UserController {

    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ISmsCodeService smsCodeService;

    @Anonymous
    @GetMapping("/sendSmsCode")
    @Operation(summary = "发送注册短信验证码", description = "向指定手机号发送注册短信验证码")
    public R<Void> sendSmsCode(@RequestParam("phone") String phone) {
        smsCodeService.sendRegisterCode(phone);
        return R.ok();
    }

    @Anonymous
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册")
    public R<Void> register(@RequestBody RegisterBody user) {
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? R.ok() : R.fail(msg);
    }

    @PostMapping("/updateUser")
    @Operation(summary = "修改用户信息", description = "修改用户信息")
    public R<?> updateUser(@RequestBody UpdateUserRequest request) {
        return R.ok(userService.updateUser(request));
    }
}
