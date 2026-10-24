package com.rally.controller;

import com.rally.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口：确认后端、MySQL、Redis 都正常。
 * 访问 http://localhost:8080/api/health
 *
 * Controller ≈ 前端的路由处理函数：一个 URL 对应一个方法，方法的返回值会被自动转成 JSON。
 */
@Slf4j
@RestController
@RequiredArgsConstructor // Lombok：为 final 字段生成构造方法，Spring 通过构造方法把依赖"注入"进来
public class HealthController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Spring 提供的执行 SQL 的工具，这里只用来测试数据库连接 */
    private final JdbcTemplate jdbcTemplate;

    /** Spring 提供的操作 Redis 的工具 */
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "rally-server");
        data.put("time", LocalDateTime.now().format(TIME_FORMAT));
        data.put("mysql", checkMysql());
        data.put("redis", checkRedis());
        return Result.success(data);
    }

    private String checkMysql() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            log.warn("MySQL 连接失败：{}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) RedisConnection::ping);
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("Redis 连接失败：{}", e.getMessage());
            return "DOWN";
        }
    }
}
