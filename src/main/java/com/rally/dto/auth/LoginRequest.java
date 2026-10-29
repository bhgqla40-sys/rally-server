package com.rally.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录请求体，对应前端 api/auth.ts 的 LoginForm */
public record LoginRequest(
        @NotBlank(message = "请输入手机号")
        String phone,

        @NotBlank(message = "请输入密码")
        String password,

        /*
         * 可选：在小程序里用手机号登录时，顺便传 uni.login() 拿到的 code，
         * 后端会把这个微信绑定到手机号账号上，以后点「微信一键登录」就进的是这个账号。H5 不用传。
         */
        @Size(max = 128, message = "微信登录凭证格式不对")
        String wxCode
) {
}
