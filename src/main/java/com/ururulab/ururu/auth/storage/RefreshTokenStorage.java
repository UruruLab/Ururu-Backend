package com.ururulab.ururu.auth.storage;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Redis Lua 스크립트 - 원자적 토큰 정리
    private static final String CLEANUP_TOKENS_SCRIPT = """
            local pattern = KEYS[1]
            local maxTokens = tonumber(ARGV[1])
            local keys = redis.call('KEYS', pattern)
            local keyCount = #keys
            
            if keyCount > maxTokens then
                local tokensToDelete = keyCount - maxTokens
                local keysToDelete = {}
                
                for i = 1, tokensToDelete do
                    table.insert(keysToDelete, keys[i])
                end
                
                if #keysToDelete > 0 then
                    redis.call('DEL', unpack(keysToDelete))
                    return #keysToDelete
                end
            end
            
            return 0
            """;

    private final DefaultRedisScript<Long> cleanupScript = new DefaultRedisScript<>(CLEANUP_TOKENS_SCRIPT, Long.class);

    /**
     * Refresh Token을 저장합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     * @param refreshToken 저장할 Refresh Token
     */
    public void storeRefreshToken(final Long userId, final String userType, final String refreshToken) {
        // 원자적 토큰 정리 수행
        cleanupOldTokensAtomically(userType, userId);
        
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
     * 원자적으로 오래된 토큰을 정리합니다.
     * Redis Lua 스크립트를 사용하여 race condition을 방지합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     */
    public void cleanupOldTokensAtomically(final String userType, final Long userId) {
        final String prefix = UserType.MEMBER.getValue().equals(userType) 
            ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX 
            : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
        final String refreshKeyPattern = prefix + userId + ":*";
        
        try {
            final Long deletedCount = redisTemplate.execute(
                    cleanupScript,
                    List.of(refreshKeyPattern),
                    String.valueOf(AuthConstants.MAX_REFRESH_TOKENS_PER_USER)
            );
            
            if (deletedCount != null && deletedCount > 0) {
                log.debug("Atomically deleted {} old refresh tokens for user: {} (type: {})", deletedCount, userId, userType);
            }
        } catch (final Exception e) {
            log.warn("Failed to cleanup tokens atomically for user: {} (type: {}): {}", userId, userType, e.getMessage());
            // 원자적 정리 실패 시 기존 방식으로 fallback
            cleanupOldTokensIfNeeded(userType, userId);
        }
    }

    /**
     * 토큰 개수 제한 초과 시 오래된 토큰을 삭제합니다.
     * (fallback 방식 - 원자적 정리 실패 시 사용)
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
                // TTL 기반으로 정렬하여 가장 오래된 토큰 삭제
                final List<String> sortedKeys = getSortedKeysByTTL(keys);
                if (!sortedKeys.isEmpty()) {
                    final String oldestKey = sortedKeys.get(0);
                    redisTemplate.delete(oldestKey);
                    log.debug("Deleted oldest refresh token for user: {} (type: {}) due to limit exceeded", userId, userType);
                }
            }
        }
    }

    /**
     * 토큰 개수 제한 초과 시 강제로 오래된 토큰을 삭제합니다.
     *
     * @param userType 사용자 타입
     * @param userId 사용자 ID
     */
    public void forceCleanupOldTokens(final String userType, final Long userId) {
        final String prefix = UserType.MEMBER.getValue().equals(userType) 
            ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX 
            : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
        final String refreshKeyPattern = prefix + userId + ":*";
        final Set<String> keys = redisTemplate.keys(refreshKeyPattern);
        
        if (keys != null && keys.size() > 1) {
            // TTL 기반으로 정렬하여 오래된 토큰들을 삭제
            final List<String> sortedKeys = getSortedKeysByTTL(keys);
            final int tokensToDelete = (int) (sortedKeys.size() - AuthConstants.MAX_REFRESH_TOKENS_PER_USER + 1);
            
            if (tokensToDelete > 0) {
                final List<String> keysToDelete = sortedKeys.subList(0, tokensToDelete);
                redisTemplate.delete(keysToDelete);
                log.warn("Force deleted {} old refresh tokens for user: {} (type: {})", tokensToDelete, userId, userType);
            }
        }
    }

    /**
     * 키들을 TTL 기준으로 정렬합니다.
     * TTL이 짧을수록 (만료가 가까울수록) 앞에 위치합니다.
     *
     * @param keys 정렬할 키들
     * @return TTL 기준으로 정렬된 키 리스트
     */
    private List<String> getSortedKeysByTTL(final Set<String> keys) {
        return keys.stream()
                .filter(key -> redisTemplate.hasKey(key)) // 존재하는 키만 필터링
                .sorted(Comparator.comparingLong(key -> {
                    final Long ttl = redisTemplate.getExpire(key);
                    return ttl != null ? ttl : Long.MAX_VALUE; // TTL이 null이면 가장 뒤로
                }))
                .collect(Collectors.toList());
    }

    private String buildRefreshKey(final String userType, final Long userId, final String jti) {
        final String prefix = UserType.MEMBER.getValue().equals(userType) 
            ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX 
            : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
        return prefix + userId + ":" + jti;
    }
} 