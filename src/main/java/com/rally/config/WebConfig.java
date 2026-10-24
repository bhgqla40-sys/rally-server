package com.rally.config;

import com.rally.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册登录拦截器：默认所有接口都要登录，下面 excludePathPatterns 里的除外。
 * 路径不用写 /api 前缀（context-path 会自动去掉）。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",   // 注册、登录
                        "/health",    // 健康检查
                        "/demo/**",   // 学习示例
                        "/error"      // Spring Boot 内部的错误页
                );
    }
}
