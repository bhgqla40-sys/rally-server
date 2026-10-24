package com.rally.common;

/**
 * 当前登录用户。
 *
 * 登录拦截器（AuthInterceptor）校验完 token 后，把用户 id 存到这里；
 * 之后在 Controller / Service 里任何地方调用 UserContext.getUserId() 就能拿到"当前是谁在请求"。
 *
 * ThreadLocal：每个请求由一个独立的线程处理，ThreadLocal 里的值只对当前线程可见，
 * 所以同时有很多人请求也不会串（≈ 每个请求各自一份的"全局变量"）。
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /** 当前登录用户的 id；没登录会直接抛 401 */
    public static Long getUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }

    /** 请求结束后必须清掉，否则线程被复用时会拿到上一个人的 id */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
