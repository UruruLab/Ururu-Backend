package com.ururulab.ururu.auth.interceptor;

import com.ururulab.ururu.auth.annotation.RateLimit;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean trustForwardedHeaders;

    public RateLimitInterceptor(
            final StringRedisTemplate redisTemplate,
            @Value("${app.security.trust-forwarded-headers:false}") final boolean trustForwardedHeaders
    ) {
        this.redisTemplate = redisTemplate;
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        final RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        try {
            final String clientIp = getClientIp(request);
            final String key = "rate_limit:" + clientIp + ":" + request.getRequestURI();
            final long windowSeconds = Math.max(1, rateLimit.timeUnit().toSeconds(1));
            final Long count = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(key),
                    String.valueOf(windowSeconds)
            );
            if (count == null) {
                throw new IllegalStateException("Redis returned no rate-limit count");
            }
            if (count > rateLimit.value()) {
                response.setHeader("Retry-After", String.valueOf(windowSeconds));
                log.warn("Rate limit exceeded for IP: {}, URI: {}", clientIp, request.getRequestURI());
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
            return true;
        } catch (final BusinessException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Rate limiting failed for URI: {}", request.getRequestURI(), e);
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_FAILED);
        }
    }

    private String getClientIp(final HttpServletRequest request) {
        if (!trustForwardedHeaders) {
            return request.getRemoteAddr();
        }
        final String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        final String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }
}
