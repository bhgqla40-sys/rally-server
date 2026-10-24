package com.rally.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * JSON 格式配置（Jackson 是 Spring Boot 默认的 JSON 库，≈ JSON.stringify / JSON.parse）。
 *
 * 前端约定时间用带时区的 ISO 8601：2026-10-31T19:00:00+08:00。
 * Java 的 LocalDateTime 不带时区，默认会输出成 2026-10-31T19:00:00，
 * 这里统一加上北京时间的 +08:00，前端的 dayjs 就能准确解析。
 */
@Configuration
public class JacksonConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeWithOffset() {
        return builder -> builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.atZone(ZONE).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
    }
}
