package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.exception.InvalidRefreshTokenException;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;


    public void storeRefreshToken(final Long memberId, final String refreshToken) {
        long expirySeconds = jwtTokenProvider.getRefreshTokenExpirySeconds();
        String key = REFRESH_KEY_PREFIX + memberId;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirySeconds));
    }

    public String refreshAccessToken(final String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidRefreshTokenException("유효하지 않은 리프레시 토큰입니다.");
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidRefreshTokenException("리프레시 토큰이 아닙니다.");
        }
        String tokenId = jwtTokenProvider.getTokenId(refreshToken);
        if (isTokenBlacklisted(tokenId)) {
            throw new InvalidRefreshTokenException("만료되었거나 철회된 토큰입니다.");
        }
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String key = REFRESH_KEY_PREFIX + memberId;
        String storedToken = redisTemplate.opsForValue().get(key);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new InvalidRefreshTokenException("이미 사용되었거나 유효하지 않은 토큰입니다.");
        }
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                memberId,
                jwtTokenProvider.getEmail(refreshToken),
                jwtTokenProvider.getRole(refreshToken)
        );
        return newAccessToken;
    }

    public void logout(final Long memberId, final String accessToken) {
        String refreshKey = REFRESH_KEY_PREFIX + memberId;
        redisTemplate.delete(refreshKey);
        String tokenId = jwtTokenProvider.getTokenId(accessToken);
        long expiry = jwtTokenProvider.getRemainingExpiry(accessToken);
        if (tokenId != null && expiry > 0) {
            String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;
            redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofSeconds(expiry));
        }
    }

    public boolean isTokenBlacklisted(final String tokenId) {
        if (tokenId == null) return false;
        String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
}
