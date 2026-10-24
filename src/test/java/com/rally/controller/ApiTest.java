package com.rally.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口测试：不用真的启动服务器，模拟发请求，检查返回结果。
 * 注意：这里的路径不带 /api 前缀（测试环境不经过 context-path）。
 */
@WebMvcTest
class ApiTest {

    @Autowired
    private MockMvc mockMvc;

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
