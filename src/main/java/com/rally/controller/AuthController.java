package com.rally.controller;

import com.rally.common.Result;
import com.rally.dto.auth.LoginRequest;
import com.rally.dto.auth.LoginResult;
import com.rally.dto.auth.RegisterRequest;
import com.rally.dto.auth.WxLoginRequest;
import com.rally.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 注册登录接口（不需要登录就能访问，见 WebConfig） */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/register  { phone, password, nickname, wxCode? } */
    @PostMapping("/register")
    public Result<LoginResult> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /** POST /api/auth/login  { phone, password, wxCode? }  小程序里传 wxCode 会顺便绑定微信 */
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /** POST /api/auth/wx-login  { code }  小程序一键登录，新用户自动建账号 */
    @PostMapping("/wx-login")
    public Result<LoginResult> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.success(authService.wxLogin(request));
    }
}
