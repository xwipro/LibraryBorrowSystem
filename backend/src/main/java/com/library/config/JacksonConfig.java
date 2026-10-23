package com.library.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 3 (Spring Boot 4) 时间序列化统一格式(与前端 mock 数据一致):
 * - LocalDateTime -> yyyy-MM-dd HH:mm:ss
 * - LocalDate     -> yyyy-MM-dd
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME));
            module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE));
            builder.addModule(module);
        };
    }
}
