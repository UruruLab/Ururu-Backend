package com.ururulab.ururu.global.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    private static final int CONNECTION_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 10000;
    private static final int MAX_TOTAL_CONNECTIONS = 100;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
    private static final int CONNECTION_REQUEST_TIMEOUT_MILLIS = 3000;

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
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT_MILLIS))
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
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Bean
    public RestClient restClient(final HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        return RestClient.builder()
                .requestFactory(httpRequestFactory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestTemplate restTemplate(final HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        return new RestTemplate(httpRequestFactory);
    }
}
