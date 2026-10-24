package com.rally.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具：登录成功时生成 token，之后每次请求校验 token。
 *
 * JWT 就是一段带签名的字符串，里面存了用户 id 和过期时间。
 * 签名用的密钥只有后端知道，所以前端改不了里面的内容（改了签名就对不上），
 * 后端也不用把 token 存数据库，验一下签名就知道是谁。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(@Value("${rally.jwt.secret}") String secret,
                   @Value("${rally.jwt.expire-days:30}") long expireDays) {
        // HS256 要求密钥至少 32 字节，太短启动时会直接报错
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofDays(expireDays);
    }

    /** 给某个用户生成 token */
    public String createToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
    }

    /** 解析 token 得到用户 id；token 被篡改、格式不对或已过期都返回 null */
    public Long parseUserId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
