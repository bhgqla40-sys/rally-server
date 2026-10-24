package com.rally;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 最基础的测试：检查整个 Spring 应用能不能正常启动。
 * 会真的连接数据库和 Redis，所以跑测试前要先 docker compose up -d。
 */
@SpringBootTest
class RallyServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
