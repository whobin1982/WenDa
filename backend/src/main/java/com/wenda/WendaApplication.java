package com.wenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wenda 后端启动入口。
 *
 * <p>基线：Java 21 + Spring Boot 3.3.x（基线索引 v1.0 + 架构 v0.3 §4.2 + 技术方案 v0.4 §2.1）。
 * <p>开源：Apache-2.0；JDK 选用 OpenJDK 发行版（Eclipse Temurin / Amazon Corretto / Azul Zulu）。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class WendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(WendaApplication.class, args);
    }
}
