package com.rally.interceptor;

import com.rally.common.UnauthorizedException;
import com.rally.common.UserContext;
import com.rally.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器 ≈ 前端路由守卫（router.beforeEach）。
 * 请求进入 Controller 之前先到这里：从请求头取出 token 校验，
 * 通过就把用户 id 放进 UserContext，不通过直接返回 401。
 *
 * 哪些接口不需要登录，在 WebConfig 里配置。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 不是我们写的接口方法（比如浏览器跨域预检的 OPTIONS 请求），直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 前端 request.ts 的写法：Authorization: Bearer <token>
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException();
        }

        Long userId = jwtUtil.parseUserId(header.substring(BEARER_PREFIX.length()));
        if (userId == null) {
            throw new UnauthorizedException();
        }

        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
