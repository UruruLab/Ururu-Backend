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

        try {
            String clientIp = getClientIp(request);
            String key = "rate_limit:" + clientIp + ":" + request.getRequestURI();
            
            // 원자적으로 카운트 증가 (동시성 문제 해결)
            Long count = redisTemplate.opsForValue().increment(key);
            
            // null 체크 추가
            if (count == null) {
                log.error("Failed to increment rate limit counter for IP: {}, URI: {}", clientIp, request.getRequestURI());
                return true; // Redis 오류 시 요청 허용
            }
            
            // 첫 번째 요청인 경우에만 만료 시간 설정
            if (count == 1) {
                redisTemplate.expire(key, Duration.of(1, rateLimit.timeUnit().toChronoUnit()));
            }
            
            if (count > rateLimit.value()) {
                log.warn("Rate limit exceeded for IP: {}, URI: {}", clientIp, request.getRequestURI());
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
            
            return true;
        } catch (BusinessException e) {
            // Rate limit 초과는 그대로 예외 전파
            throw e;
        } catch (Exception e) {
            log.error("Rate limiting failed for URI: {}, allowing request", request.getRequestURI(), e);
            // fail-open: Redis 오류 시 요청 허용
            return true;
        }
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