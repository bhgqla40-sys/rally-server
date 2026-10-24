package com.rally.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一的接口返回格式，所有接口都返回这个结构，前端好统一处理：
 *
 * <pre>
 * {
 *   "code": 0,            // 0 = 成功，其他 = 失败（见 ResultCode）
 *   "message": "成功",     // 提示信息，失败时可以直接 toast 给用户
 *   "data": { ... }       // 真正的数据
 * }
 * </pre>
 *
 * 前端对应的 TS 类型：
 * <pre>
 * interface Result&lt;T&gt; { code: number; message: string; data: T }
 * </pre>
 *
 * @param <T> data 的类型（Java 的泛型 ≈ TS 的泛型）
 */
@Data               // Lombok：自动生成 getter/setter/toString 等
@NoArgsConstructor  // Lombok：自动生成无参构造方法
@AllArgsConstructor // Lombok：自动生成全参构造方法
public class Result<T> {

    private int code;
    private String message;
    private T data;

    /** 成功，带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功，不带数据 */
    public static <T> Result<T> success() {
        return success(null);
    }

    /** 失败，使用预定义的返回码 */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败，自定义提示信息 */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
