package com.rally.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 小程序一键登录：{ code }，code 是小程序里 uni.login() 拿到的临时登录凭证 */
public record WxLoginRequest(
        @NotBlank(message = "缺少微信登录凭证")
        @Size(max = 128, message = "微信登录凭证格式不对")
        String code
) {
}
