package com.rally.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理 ≈ 前端 axios 的响应拦截器，只不过是在后端这一侧。
 * 任何接口里抛出的异常都会被这里捕获，统一转成 Result 格式返回，
 * 这样前端永远拿到的是 { code, message, data }，不会收到一堆 Java 报错堆栈。
 *
 * 约定：
 * - 业务异常（BusinessException）：HTTP 状态码 200，靠 body 里的 code 区分
 * - 参数错误 / 接口不存在 / 服务器异常：返回对应的 HTTP 状态码，body 同样是 Result 格式
 */
@Slf4j // Lombok：自动生成 log 对象，用来打日志
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：代码里主动 throw new BusinessException("...") */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：@Valid 校验不通过（比如 @NotBlank 的字段传了空） */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getDefaultMessage()
                : ResultCode.BAD_REQUEST.getMessage();
        log.warn("参数校验失败：{}", message);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** 缺少必填的 URL 参数（?xxx=） */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少参数：{}", e.getParameterName());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "缺少参数：" + e.getParameterName());
    }

    /** 请求体不是合法的 JSON，或者字段类型对不上 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    /** 请求的接口地址不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.error(ResultCode.NOT_FOUND);
    }

    /** 请求方法不对，比如接口是 POST，前端用了 GET */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return Result.error(ResultCode.METHOD_NOT_ALLOWED);
    }

    /** 兜底：其他所有没预料到的异常（真正的 bug），完整堆栈打到日志里方便排查 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.SERVER_ERROR);
    }
}
