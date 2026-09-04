package com.ururulab.ururu.auth.storage;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 토큰 블랙리스트 저장소.
 * Redis를 사용한 토큰 블랙리스트 관리를 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TokenBlacklistStorage {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 토큰이 블랙리스트에 있는지 확인합니다.
     *
     * @param tokenId 확인할 토큰 ID
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isTokenBlacklisted(final String tokenId) {
        if (tokenId == null) {
            return false;
        }
        
        final String blacklistKey = AuthConstants.BLACKLIST_KEY_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    public void revokeUserTokens(final Long userId, final String userType) {
        if (userId == null || userType == null || userType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN_BLACKLIST_PARAMETERS);
        }
        try {
            redisTemplate.opsForValue().set(
                    buildUserRevocationKey(userId, userType),
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(jwtTokenProvider.getRefreshTokenExpirySeconds())
            );
        } catch (final Exception e) {
            log.error("Failed to revoke access tokens for user {}", userId, e);
            throw new BusinessException(ErrorCode.TOKEN_BLACKLIST_OPERATION_FAILED);
        }
    }

    public boolean isUserTokenRevoked(
            final Long userId,
            final String userType,
            final long issuedAtMillis
    ) {
        try {
            final String revokedAt = redisTemplate.opsForValue().get(buildUserRevocationKey(userId, userType));
            return revokedAt != null && issuedAtMillis <= Long.parseLong(revokedAt);
        } catch (final Exception e) {
            log.error("Failed to check token revocation for user {}", userId, e);
            throw new BusinessException(ErrorCode.TOKEN_BLACKLIST_OPERATION_FAILED);
        }
    }

    private String buildUserRevocationKey(final Long userId, final String userType) {
        return AuthConstants.USER_REVOCATION_KEY_PREFIX + userType + ":" + userId;
    }

}
