package com.example.demo.resttemplate;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * 使用 RestTemplateBuilder 构建 RestTemplate
     * Spring Boot 4 中 HttpComponentsClientHttpRequestFactory 的 setConnectTimeout 已移除
     * 通过 RestTemplateBuilder 统一管理超时配置
     * 底层自动检测 HttpClient 5（需在 classpath 中）
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
