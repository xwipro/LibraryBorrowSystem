package com.library.controller;

import com.library.common.Result;
import com.library.dto.auth.*;
import com.library.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口: 登录 / 登出 / 用户信息 / 验证码 / 找回密码
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 用户登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    /** 退出登录(将当前 token 拉黑) */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /** 当前登录用户信息 */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.ok(authService.info());
    }

    /** 获取登录验证码图片(base64) */
    @GetMapping("/captcha")
    public Result<CaptchaResult> captcha() {
        return Result.ok(authService.captcha());
    }

    /** 找回密码 - 发送验证码到邮箱(异步) */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                       HttpServletRequest request) {
        authService.forgotPassword(req.getEmail());
        return Result.ok();
    }

    /** 找回密码 - 重置密码 */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return Result.ok();
    }

    /** 个人中心 - 修改当前登录用户密码(校验旧密码) */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return Result.ok();
    }
}
