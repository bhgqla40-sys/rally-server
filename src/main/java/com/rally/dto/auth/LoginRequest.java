package com.rally.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** 登录请求体，对应前端 api/auth.ts 的 LoginForm */
public record LoginRequest(
        @NotBlank(message = "请输入手机号")
        String phone,

        @NotBlank(message = "请输入密码")
        String password
) {
}
