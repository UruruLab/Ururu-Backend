package com.ururulab.ururu.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 클라이언트 설정.
 * RestClient Bean을 등록하여 동기적 HTTP 통신을 처리합니다.
 */
@Configuration
public class HttpClientConfig {

    private static final int CONNECTION_TIMEOUT = 5000; // 5초
    private static final int READ_TIMEOUT = 10000; // 10초

    @Bean
    public RestClient restClient() {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        return new RestTemplate(factory);
    }
}