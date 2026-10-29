package com.rally.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 解析微信 code2Session 的返回（不真的调微信） */
class WxClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void success() {
        String body = "{\"session_key\":\"abc==\",\"openid\":\"oAbC123\"}";
        assertEquals("oAbC123", WxClient.parseOpenid(mapper, body));
    }

    @Test
    void invalidCode() {
        String body = "{\"errcode\":40029,\"errmsg\":\"invalid code, rid: 1\"}";
        BusinessException e = assertThrows(BusinessException.class, () -> WxClient.parseOpenid(mapper, body));
        assertEquals("登录凭证无效或已过期，请重新登录", e.getMessage());
    }

    @Test
    void codeUsedTwiceAndRateLimit() {
        assertEquals("登录凭证无效或已过期，请重新登录", WxClient.errorMessage(40163));
        assertEquals("登录太频繁了，请稍后再试", WxClient.errorMessage(45011));
        assertEquals("微信登录失败（12345），请稍后再试", WxClient.errorMessage(12345));
    }

    @Test
    void notJsonOrNoOpenid() {
        assertThrows(BusinessException.class, () -> WxClient.parseOpenid(mapper, "<html>502</html>"));
        assertThrows(BusinessException.class, () -> WxClient.parseOpenid(mapper, "{}"));
        assertThrows(BusinessException.class, () -> WxClient.parseOpenid(mapper, null));
    }
}
