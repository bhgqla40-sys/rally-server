package com.rally.common;

import lombok.Getter;

/**
 * 业务异常：业务上的"失败"（比如"活动人数已满"）就 throw 这个。
 * 会被 GlobalExceptionHandler 统一捕获，转成 Result 返回给前端。
 *
 * 用法：
 * <pre>
 * throw new BusinessException("活动人数已满");
 * throw new BusinessException(ResultCode.UNAUTHORIZED);
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
