package com.ururulab.ururu.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * HTTP 클라이언트 설정.
 *
 * <p>WebClient와 RestTemplate Bean을 등록합니다.</p>
 */
@Configuration
public class HttpClientConfig {

    private static final int CONNECTION_TIMEOUT = 5000; // 5초
    private static final int READ_TIMEOUT = 10000; // 10초

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
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