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

@Service
@RequiredArgsConstructor
public final class JwtRefreshService {
    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    public void storeRefreshToken(final Long memberId, final String refreshToken) {
        final long expirySeconds = jwtTokenProvider.getRefreshTokenExpirySeconds();
        final String key = REFRESH_KEY_PREFIX + memberId;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirySeconds));
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

        final Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        final String key = REFRESH_KEY_PREFIX + memberId;
        final String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        final String email = jwtTokenProvider.getEmail(refreshToken);
        final String role = jwtTokenProvider.getRole(refreshToken);

        final String newAccessToken = jwtTokenProvider.generateAccessToken(memberId, email, role);

        return SocialLoginResponse.of(
                newAccessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiry(),
                SocialLoginResponse.MemberInfo.of(memberId, email, null, null)
        );
    }

    public void logout(final String authorization) {
        final String accessToken = extractTokenFromBearer(authorization);
        final Long memberId = jwtTokenProvider.getMemberId(accessToken);

        final String refreshKey = REFRESH_KEY_PREFIX + memberId;
        redisTemplate.delete(refreshKey);

        final String tokenId = jwtTokenProvider.getTokenId(accessToken);
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

    private String extractTokenFromBearer(final String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }
        return bearerToken.substring(BEARER_PREFIX.length());
    }
}