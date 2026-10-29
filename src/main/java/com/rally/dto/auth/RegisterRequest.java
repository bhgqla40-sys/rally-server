package com.rally.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体，对应前端 api/auth.ts 的 RegisterForm。
 * 校验规则和前端登录页保持一致：前端拦一道，后端再拦一道（前端的校验可以被绕过，后端必须自己校验）。
 */
public record RegisterRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号")
        String phone,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 32, message = "密码需要 6 到 32 位")
        String password,

        @NotBlank(message = "请填写昵称")
        @Size(max = 12, message = "昵称最多 12 个字")
        String nickname,

        /* 可选：在小程序里注册时传 uni.login() 拿到的 code，注册后顺便绑定微信。H5 不用传 */
        @Size(max = 128, message = "微信登录凭证格式不对")
        String wxCode
) {
}
