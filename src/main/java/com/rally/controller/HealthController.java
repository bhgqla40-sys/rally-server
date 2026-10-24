package com.rally.controller;

import com.rally.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口：用来确认后端活着。
 * 访问 http://localhost:8080/api/health
 *
 * Controller ≈ 前端的路由处理函数：一个 URL 对应一个方法，方法的返回值会被自动转成 JSON。
 */
@RestController
public class HealthController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "rally-server");
        data.put("time", LocalDateTime.now().format(TIME_FORMAT));
        return Result.success(data);
    }
}
