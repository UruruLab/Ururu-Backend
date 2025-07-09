package com.ururulab.ururu.global.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 클라이언트 설정.
 *
 * <p>소셜 로그인 API 호출을 위한 안정적인 RestClient 설정을 제공합니다.
 * 타임아웃, 연결 풀, 네트워크 안정성 설정이 포함됩니다.</p>
 */
@Configuration
public class HttpClientConfig {

    private static final int READ_TIMEOUT_MILLIS = 10000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MILLIS = 3000;
    private static final int MAX_TOTAL_CONNECTIONS = 100;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        return connectionManager;
    }

    @Bean
    public RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_REQUEST_TIMEOUT_MILLIS))
                .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MILLIS))
                .build();
    }

    @Bean
    public HttpClient httpClient(final PoolingHttpClientConnectionManager connectionManager,
                                 final RequestConfig requestConfig) {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory(final HttpClient httpClient) {
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        return factory;
    }

    @Bean
    public RestClient restClient(final HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        return RestClient.builder()
                .requestFactory(httpRequestFactory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean("socialLoginRestClient")
    public RestClient socialLoginRestClient(final HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        return RestClient.builder()
                .requestFactory(httpRequestFactory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "UruruApp/1.0")
                .build();
    }

    @Bean
    public RestTemplate restTemplate(final HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(httpRequestFactory);
        return restTemplate;
    }

    private static final int AI_SERVICE_READ_TIMEOUT_MILLIS = 45000;
    private static final int AI_SERVICE_CONNECTION_TIMEOUT_MILLIS = 10000;

    @Bean("aiServiceConnectionManager")
    public PoolingHttpClientConnectionManager aiServiceConnectionManager() {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        return connectionManager;
    }

    @Bean("aiServiceRequestConfig")
    public RequestConfig aiServiceRequestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(AI_SERVICE_CONNECTION_TIMEOUT_MILLIS))
                .setResponseTimeout(Timeout.ofMilliseconds(AI_SERVICE_READ_TIMEOUT_MILLIS))
                .build();
    }

    @Bean("aiServiceHttpClient")
    public HttpClient aiServiceHttpClient(
            @Qualifier("aiServiceConnectionManager") final PoolingHttpClientConnectionManager connectionManager,
            @Qualifier("aiServiceRequestConfig") final RequestConfig requestConfig
    ) {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Bean("aiServiceHttpRequestFactory")
    public HttpComponentsClientHttpRequestFactory aiServiceHttpRequestFactory(
            @Qualifier("aiServiceHttpClient") final HttpClient httpClient
    ) {
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        return factory;
    }

    @Bean("aiServiceRestClient")
    public RestClient aiServiceRestClient(
            @Value("${ai.service.url:http://localhost:8000}") final String aiServiceUrl,
            @Qualifier("aiServiceHttpRequestFactory") final HttpComponentsClientHttpRequestFactory httpRequestFactory
    ) {
        return RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(httpRequestFactory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}