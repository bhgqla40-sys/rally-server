package com.rally.controller;

import com.rally.common.Result;
import com.rally.dto.auth.LoginRequest;
import com.rally.dto.auth.LoginResult;
import com.rally.dto.auth.RegisterRequest;
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

    /** POST /api/auth/register  { phone, password, nickname } */
    @PostMapping("/register")
    public Result<LoginResult> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /** POST /api/auth/login  { phone, password } */
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
