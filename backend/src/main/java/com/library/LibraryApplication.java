package com.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 图书管理系统 2026 - 后端启动类
 * 技术栈: Spring Boot 4 + MyBatis-Plus + PostgreSQL + Redis + JWT
 */
@EnableAsync
@MapperScan("com.library.mapper")
@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
