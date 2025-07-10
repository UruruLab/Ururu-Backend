package com.ururulab.ururu.auth.storage;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Refresh Token 저장소.
 * Redis를 사용한 Refresh Token 저장/조회/삭제를 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class RefreshTokenStorage {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token을 저장합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     * @param refreshToken 저장할 Refresh Token
     */
    public void storeRefreshToken(final Long userId, final String userType, final String refreshToken) {
        // 토큰 개수 제한 확인 및 정리
        cleanupOldTokensIfNeeded(userType, userId);
        
        final long expirySeconds = jwtTokenProvider.getRefreshTokenExpirySeconds();
        final String jti = jwtTokenProvider.getTokenId(refreshToken);
        final String key = buildRefreshKey(userType, userId, jti);
        
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirySeconds));
        log.debug("Refresh token stored for user: {} (type: {}), expiry: {} seconds", userId, userType, expirySeconds);
    }

    /**
     * 저장된 Refresh Token을 조회합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @return 저장된 Refresh Token, 없으면 null
     */
    public String getRefreshToken(final String userType, final Long userId, final String tokenId) {
        final String key = buildRefreshKey(userType, userId, tokenId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 특정 Refresh Token을 삭제합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     * @param tokenId 삭제할 토큰 ID
     */
    public void deleteRefreshToken(final String userType, final Long userId, final String tokenId) {
        final String key = buildRefreshKey(userType, userId, tokenId);
        redisTemplate.delete(key);
        log.debug("Deleted refresh token for user: {} (type: {}), tokenId: {}", userId, userType, tokenId);
    }

    /**
     * 사용자의 모든 Refresh Token을 삭제합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     */
    public void deleteAllRefreshTokens(final String userType, final Long userId) {
        final String prefix = UserType.MEMBER.getValue().equals(userType) 
            ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX 
            : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
        final String refreshKeyPattern = prefix + userId + ":*";
        final Set<String> keys = redisTemplate.keys(refreshKeyPattern);
        
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Deleted {} refresh tokens for user: {} (type: {})", keys.size(), userId, userType);
        }
    }

    /**
     * 사용자의 Refresh Token 개수를 확인합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     * @return Refresh Token 개수
     */
    public long getRefreshTokenCount(final String userType, final Long userId) {
        final String prefix = UserType.MEMBER.getValue().equals(userType) 
            ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX 
            : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
        final String refreshKeyPattern = prefix + userId + ":*";
        final Set<String> keys = redisTemplate.keys(refreshKeyPattern);
        return keys != null ? keys.size() : 0;
    }

    /**
     * Refresh Token이 최대 개수 제한을 초과했는지 확인합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     * @return 최대 개수 제한을 초과했으면 true
     */
    public boolean isRefreshTokenLimitExceeded(final String userType, final Long userId) {
        return getRefreshTokenCount(userType, userId) >= AuthConstants.MAX_REFRESH_TOKENS_PER_USER;
    }

    /**
     * 토큰 개수 제한 초과 시 오래된 토큰을 삭제합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     */
    public void cleanupOldTokensIfNeeded(final String userType, final Long userId) {
        final long currentCount = getRefreshTokenCount(userType, userId);
        if (currentCount >= AuthConstants.MAX_REFRESH_TOKENS_PER_USER) {
            final String prefix = UserType.MEMBER.getValue().equals(userType) 
                ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX 
                : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
            final String refreshKeyPattern = prefix + userId + ":*";
            final Set<String> keys = redisTemplate.keys(refreshKeyPattern);
            
            if (keys != null && keys.size() > 1) {
                // 가장 오래된 토큰 하나를 삭제 (최소 1개는 유지)
                final String oldestKey = keys.stream()
                    .sorted()
                    .findFirst()
                    .orElse(null);
                
                if (oldestKey != null) {
                    redisTemplate.delete(oldestKey);
                    log.debug("Deleted oldest refresh token for user: {} (type: {}) due to limit exceeded", userId, userType);
                }
            }
        }
    }

    private String buildRefreshKey(final String userType, final Long userId, final String jti) {
        final String prefix = UserType.MEMBER.getValue().equals(userType) 
            ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX 
            : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
        return prefix + userId + ":" + jti;
    }
} 