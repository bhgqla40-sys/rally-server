package com.rally.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置（CORS）。
 * 开发时前端跑在 http://localhost:5173 之类的地址，后端在 http://localhost:8080，
 * 端口不同就算跨域，浏览器会拦截。这里允许本机和局域网（手机真机调试）的地址访问。
 *
 * 如果前端已经用 vite 的 server.proxy 做了代理，这个配置不生效也没关系，两者不冲突。
 * 上线后前后端通常同域名（Nginx 转发），到时候再按实际域名调整。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://192.168.*.*:*"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
