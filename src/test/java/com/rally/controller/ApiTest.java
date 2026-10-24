package com.rally.controller;

import com.rally.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口测试：不用真的启动服务器，模拟发请求，检查返回结果。
 * 注意：这里的路径不带 /api 前缀（测试环境不经过 context-path）。
 *
 * WebMvcTest 只加载 Controller 这一层，不连数据库和 Redis，
 * Controller 依赖的数据库 / Redis 工具用 @MockitoBean 造一个"假的"顶上（≈ 前端测试里的 vi.mock）。
 */
@WebMvcTest(controllers = {HealthController.class, DemoController.class})
class ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    /** 登录拦截器依赖它（这几个接口都不需要登录，用不到，给个假的就行） */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void healthReturnsSuccess() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void echoRejectsBlankName() throws Exception {
        mockMvc.perform(post("/demo/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"maxPeople\": 8}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void echoAcceptsValidBody() throws Exception {
        mockMvc.perform(post("/demo/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"badminton\", \"maxPeople\": 8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.maxPeople").value(8));
    }

    @Test
    void businessErrorUsesBusinessCode() throws Exception {
        mockMvc.perform(get("/demo/fail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }
}
