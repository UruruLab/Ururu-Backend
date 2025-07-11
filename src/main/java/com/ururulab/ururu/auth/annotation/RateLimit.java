package com.ururulab.ururu.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting을 위한 어노테이션.
 * 지정된 시간 동안 최대 요청 수를 제한합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    
    /**
     * 최대 요청 수
     */
    int value() default 5;
    
    /**
     * 키 생성 전략 (기본값: IP 주소)
     */
    String keyStrategy() default "IP";
} 