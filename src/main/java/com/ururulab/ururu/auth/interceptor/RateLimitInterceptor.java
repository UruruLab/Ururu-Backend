package com.ururulab.ururu.auth.interceptor;

import com.ururulab.ururu.auth.annotation.RateLimit;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting을 처리하는 인터셉터.
 * Redis를 사용하여 요청 수를 제한합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return true;
        }

        String clientIp = getClientIp(request);
        String key = "rate_limit:" + clientIp + ":" + request.getRequestURI();
        
        // 현재 요청 수 확인
        String currentCount = redisTemplate.opsForValue().get(key);
        int count = currentCount == null ? 0 : Integer.parseInt(currentCount);
        
        if (count >= rateLimit.value()) {
            log.warn("Rate limit exceeded for IP: {}, URI: {}", clientIp, request.getRequestURI());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        
        // 요청 수 증가
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofSeconds(rateLimit.timeUnit().toSeconds(1)));
        
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 