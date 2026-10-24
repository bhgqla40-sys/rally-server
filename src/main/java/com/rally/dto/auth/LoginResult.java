package com.rally.dto.auth;

/** 登录 / 注册成功后返回给前端的数据，对应前端 api/auth.ts 的 LoginResult */
public record LoginResult(
        String token,
        Long userId,
        String nickname
) {
}
