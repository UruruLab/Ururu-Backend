package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public final class JwtRefreshService {
    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    public void storeRefreshToken(final Long userId, final String userType, final String refreshToken) {
        final long expirySeconds = jwtTokenProvider.getRefreshTokenExpirySeconds();
        final String jti = jwtTokenProvider.getTokenId(refreshToken);
        final String key = buildRefreshKey(userType, userId, jti);
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirySeconds));
    }

    // 기존 메서드 호환성을 위한 오버로드
    public void storeRefreshToken(final Long memberId, final String refreshToken) {
        storeRefreshToken(memberId, "MEMBER", refreshToken);
    }

    public SocialLoginResponse refreshAccessToken(final String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        final String tokenId = jwtTokenProvider.getTokenId(refreshToken);
        if (isTokenBlacklisted(tokenId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        final Long userId = jwtTokenProvider.getMemberId(refreshToken);
        final String userType = jwtTokenProvider.getUserType(refreshToken);
        final String key = buildRefreshKey(userType, userId, tokenId);
        final String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        final String email = jwtTokenProvider.getEmail(refreshToken);
        final String role = jwtTokenProvider.getRole(refreshToken);

        final String newAccessToken = jwtTokenProvider.generateAccessToken(userId, email, role, userType);

        return SocialLoginResponse.of(
                newAccessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiry(),
                SocialLoginResponse.MemberInfo.of(userId, email, null, null)
        );
    }

    public void logout(final String authorization) {
        final String accessToken = extractTokenFromBearer(authorization);
        final Long userId = jwtTokenProvider.getMemberId(accessToken);
        final String userType = jwtTokenProvider.getUserType(accessToken);
        final String tokenId = jwtTokenProvider.getTokenId(accessToken);

        // Redis에서 해당 사용자의 모든 refresh token 삭제
        final String refreshKeyPattern = REFRESH_KEY_PREFIX + userType + ":" + userId + ":*";
        final Set<String> keys = redisTemplate.keys(refreshKeyPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // Access token을 블랙리스트에 추가
        final long expiry = jwtTokenProvider.getRemainingExpiry(accessToken);
        if (tokenId != null && expiry > 0) {
            final String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;
            redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofSeconds(expiry));
        }
    }

    public boolean isTokenBlacklisted(final String tokenId) {
        if (tokenId == null) return false;
        final String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    private String buildRefreshKey(final String userType, final Long userId, final String jti) {
        return REFRESH_KEY_PREFIX + userType + ":" + userId + ":" + jti;
    }

    private String extractTokenFromBearer(final String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }
        return bearerToken.substring(BEARER_PREFIX.length());
    }
}