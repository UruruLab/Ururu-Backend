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

        // Redis에서 사용자 정보 가져오기 (이메일, 역할 등)
        final String userInfoKey = "user_info:" + userType + ":" + userId;
        final String userInfo = redisTemplate.opsForValue().get(userInfoKey);
        
        String email = null;
        String role = null;
        
        // 판매자 로그인의 경우 Redis에서 사용자 정보 가져오기
        if ("SELLER".equals(userType) && userInfo != null && !userInfo.isEmpty()) {
            // 안전한 JSON 파싱
            try {
                // "email":"sample@ururu.com" 부분 추출
                if (userInfo.contains("\"email\":")) {
                    String emailPart = userInfo.split("\"email\":")[1];
                    email = emailPart.split(",")[0].replace("\"", "").trim();
                }
                
                // "role":"SELLER" 부분 추출
                if (userInfo.contains("\"role\":")) {
                    String rolePart = userInfo.split("\"role\":")[1];
                    role = rolePart.split("}")[0].replace("\"", "").trim();
                }
            } catch (Exception e) {
                // log.warn("Failed to parse user info from Redis: {}", userInfo, e);
            }
        }
        
        // 소셜 로그인의 경우 또는 Redis에서 정보를 가져오지 못한 경우 기본값 사용
        if (email == null || email.isEmpty()) {
            // 소셜 로그인의 경우 기본 이메일 사용
            email = "social@ururu.com";
        }
        if (role == null || role.isEmpty()) {
            role = userType.equals("SELLER") ? "SELLER" : "MEMBER";
        }

        // 새로운 access token 생성
        final String newAccessToken = jwtTokenProvider.generateAccessToken(userId, email, role, userType);
        
        // 새로운 refresh token 생성 (토큰 로테이션)
        final String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, userType);
        
        // 기존 refresh token을 블랙리스트에 추가
        final long refreshExpiry = jwtTokenProvider.getRemainingExpiry(refreshToken);
        if (refreshExpiry > 0) {
            final String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;
            redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofSeconds(refreshExpiry));
        }
        
        // 새로운 refresh token을 Redis에 저장
        storeRefreshToken(userId, userType, newRefreshToken);

        return SocialLoginResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessTokenExpiry(),
                SocialLoginResponse.MemberInfo.of(userId, email, null, null, userType)
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