package com.rally.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 纯 Java 单元测试，不需要启动 Spring，跑得很快 */
class JwtUtilTest {

    private static final String SECRET = "test-secret-at-least-32-characters-long!!";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 30);

    @Test
    void tokenRoundTrip() {
        String token = jwtUtil.createToken(42L);
        assertEquals(42L, jwtUtil.parseUserId(token));
    }

    @Test
    void garbageTokenIsRejected() {
        assertNull(jwtUtil.parseUserId("not-a-token"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtUtil other = new JwtUtil("another-secret-that-is-also-32-chars-long", 30);
        assertNull(jwtUtil.parseUserId(other.createToken(42L)));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil alreadyExpired = new JwtUtil(SECRET, -1);
        assertNull(jwtUtil.parseUserId(alreadyExpired.createToken(42L)));
    }
}
