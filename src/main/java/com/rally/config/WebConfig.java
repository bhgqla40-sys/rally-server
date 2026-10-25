package com.rally.config;

import com.rally.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

/**
 * Web 相关配置：
 * 1. 登录拦截器：默认所有接口都要登录，excludePathPatterns 里的除外
 * 2. 上传文件的访问：把上传目录映射成 /api/uploads/**（≈ 前端的 public 目录）
 *
 * 路径不用写 /api 前缀（context-path 会自动去掉）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String uploadDir;

    public WebConfig(AuthInterceptor authInterceptor, @Value("${rally.upload.dir}") String uploadDir) {
        this.authInterceptor = authInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",    // 注册、登录
                        "/health",     // 健康检查
                        "/demo/**",    // 学习示例
                        "/uploads/**", // 上传的图片（<image src> 加载图片时不会带 token）
                        "/error"       // Spring Boot 内部的错误页
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 和 FileStorageService 用同一个目录；末尾的 / 不能少，表示这是一个目录
        String dir = Paths.get(uploadDir).toAbsolutePath().normalize().toFile().getAbsolutePath() + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(new FileSystemResource(dir));
    }
}
