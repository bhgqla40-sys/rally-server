package com.rally.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一的返回码。前端拿到响应后看 code：0 表示成功，其他都是失败。
 * 以后有新的业务错误，在这里加一行即可。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "接口不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    BUSINESS_ERROR(1000, "操作失败"),

    SERVER_ERROR(500, "服务器开小差了，请稍后再试");

    private final int code;
    private final String message;
}
