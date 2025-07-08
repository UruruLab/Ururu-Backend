package com.ururulab.ururu.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 기능 활성화 설정.
 *
 * <p>AI 서비스 클라이언트의 @Retryable 어노테이션 동작을 위해 필요합니다.</p>
 */
@Configuration
@EnableRetry
public class RetryConfig {
}