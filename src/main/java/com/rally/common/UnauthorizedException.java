package com.rally.common;

/**
 * 未登录 / 登录已过期。
 * GlobalExceptionHandler 会把它转成 HTTP 401，前端 request.ts 收到 401 会自动退出登录。
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
    }
}
