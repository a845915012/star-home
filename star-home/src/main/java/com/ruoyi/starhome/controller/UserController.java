package com.ruoyi.starhome.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.sign.Sm4Utils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.starhome.domain.dto.ForgetPasswordBody;
import com.ruoyi.starhome.domain.dto.UpdateUserRequest;
import com.ruoyi.starhome.service.IUserService;
import com.ruoyi.starhome.sms.service.ISmsCodeService;
import com.ruoyi.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/starhome/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ISmsCodeService smsCodeService;

    @Autowired
    private ISysUserService sysUserService;

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
        // 前端SM4加密传输，此处SM4解密得到明文密码
        try {
            String decryptedPassword = Sm4Utils.decrypt(user.getPassword());
            user.setPassword(decryptedPassword);
        } catch (Exception e) {
            log.error("注册密码SM4解密失败", e);
            return R.fail("密码解密失败");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? R.ok() : R.fail(msg);
    }

    @PostMapping("/updateUser")
    @Operation(summary = "修改用户信息", description = "修改用户信息")
    public R<?> updateUser(@RequestBody UpdateUserRequest request) {
        return R.ok(userService.updateUser(request));
    }

    @Anonymous
    @PostMapping("/forgetPassword")
    @Operation(summary = "忘记密码", description = "通过手机号+短信验证码重置密码")
    public R<Void> forgetPassword(@RequestBody ForgetPasswordBody body) {
        String phone = body.getPhone();
        String password = body.getPassword();
        String smsCode = body.getSmsCode();

        // 校验手机号
        if (StringUtils.isEmpty(phone)) {
            return R.fail("手机号不能为空");
        }
        // 校验新密码
        if (StringUtils.isEmpty(password)) {
            return R.fail("新密码不能为空");
        }
        // 前端SM4加密传输，此处SM4解密得到明文密码
        String decryptedPassword;
        try {
            decryptedPassword = Sm4Utils.decrypt(password);
        } catch (Exception e) {
            log.error("忘记密码SM4解密失败", e);
            return R.fail("密码解密失败");
        }
        if (decryptedPassword.length() < 5 || decryptedPassword.length() > 20) {
            return R.fail("密码长度必须在5到20个字符之间");
        }
        // 校验短信验证码
        if (StringUtils.isEmpty(smsCode)) {
            return R.fail("短信验证码不能为空");
        }
        if (!smsCodeService.verifyCode(phone, smsCode)) {
            return R.fail("短信验证码错误或已失效");
        }

        // MD5加密新密码后存储
        String encryptedPassword = SecurityUtils.encryptPassword(decryptedPassword);
        int result = sysUserService.resetPwdByPhone(phone, encryptedPassword);
        if (result > 0) {
            return R.ok();
        } else {
            return R.fail("该手机号未注册");
        }
    }
}
