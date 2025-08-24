package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

/**
 * CSRF 토큰 관리 서비스.
 * 
 * JWT 쿠키 기반 인증에서 CSRF 공격을 방지하기 위한 토큰을 생성하고 검증합니다.
 * Redis를 사용하여 토큰을 저장하고 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class CsrfTokenService {

    private static final String CSRF_TOKEN_PREFIX = "csrf:";
    private static final int CSRF_TOKEN_LENGTH = 32;
    private static final long CSRF_TOKEN_EXPIRY_SECONDS = 3600; // 1시간

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * CSRF 토큰을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     * @return 생성된 CSRF 토큰
     */
    public String generateCsrfToken(final Long userId, final String userType) {
        final byte[] randomBytes = new byte[CSRF_TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        final String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        final String redisKey = buildCsrfTokenKey(userId, userType);
        redisTemplate.opsForValue().set(redisKey, csrfToken, Duration.ofSeconds(CSRF_TOKEN_EXPIRY_SECONDS));
        
        log.debug("CSRF 토큰 생성 완료 - userId: {}, userType: {}, token: {}...", 
                userId, userType, csrfToken.substring(0, 8));
        
        return csrfToken;
    }

    /**
     * CSRF 토큰을 검증합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     * @param csrfToken 검증할 CSRF 토큰
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public void validateCsrfToken(final Long userId, final String userType, final String csrfToken) {
        if (csrfToken == null || csrfToken.isBlank()) {
            log.warn("CSRF 토큰이 없음 - userId: {}, userType: {}", userId, userType);
            throw new BusinessException(ErrorCode.CSRF_TOKEN_MISSING);
        }

        final String redisKey = buildCsrfTokenKey(userId, userType);
        final String storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            log.warn("CSRF 토큰이 만료되었거나 존재하지 않음 - userId: {}, userType: {}", userId, userType);
            throw new BusinessException(ErrorCode.CSRF_TOKEN_EXPIRED);
        }

        if (!Objects.equals(csrfToken, storedToken)) {
            log.warn("CSRF 토큰 불일치 - userId: {}, userType: {}", userId, userType);
            throw new BusinessException(ErrorCode.CSRF_TOKEN_MISMATCH);
        }

        log.debug("CSRF 토큰 검증 성공 - userId: {}, userType: {}", userId, userType);
    }

    /**
     * CSRF 토큰을 무효화합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     */
    public void invalidateCsrfToken(final Long userId, final String userType) {
        final String redisKey = buildCsrfTokenKey(userId, userType);
        redisTemplate.delete(redisKey);
        
        log.debug("CSRF 토큰 무효화 완료 - userId: {}, userType: {}", userId, userType);
    }

    /**
     * 사용자의 모든 CSRF 토큰을 무효화합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     */
    public void invalidateAllCsrfTokens(final Long userId, final String userType) {
        final String pattern = CSRF_TOKEN_PREFIX + userType + ":" + userId + ":*";
        final var keys = redisTemplate.keys(pattern);
        
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("모든 CSRF 토큰 무효화 완료 - userId: {}, userType: {}, count: {}", 
                    userId, userType, keys.size());
        }
    }

    /**
     * CSRF 토큰 키를 생성합니다.
     */
    private String buildCsrfTokenKey(final Long userId, final String userType) {
        return CSRF_TOKEN_PREFIX + userType + ":" + userId + ":" + System.currentTimeMillis();
    }
}
