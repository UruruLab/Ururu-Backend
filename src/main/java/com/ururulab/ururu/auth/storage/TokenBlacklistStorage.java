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
     * 토큰을 블랙리스트에 추가합니다.
     *
     * @param tokenId 블랙리스트에 추가할 토큰 ID
     * @param expirySeconds 블랙리스트 유지 시간 (초)
     * @throws BusinessException 블랙리스트 추가 실패 시
     */
    public void addToBlacklist(final String tokenId, final long expirySeconds) {
        if (tokenId == null || expirySeconds <= 0) {
            log.error("Invalid blacklist parameters - tokenId: {}, expirySeconds: {}", tokenId, expirySeconds);
            throw new BusinessException(ErrorCode.INVALID_TOKEN_BLACKLIST_PARAMETERS);
        }

        try {
            final String blacklistKey = AuthConstants.BLACKLIST_KEY_PREFIX + tokenId;
            redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofSeconds(expirySeconds));
            log.debug("Token added to blacklist: {}, expiry: {} seconds", tokenId, expirySeconds);
        } catch (final Exception e) {
            log.error("Failed to add token to blacklist: {}, error: {}", tokenId, e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_BLACKLIST_OPERATION_FAILED);
        }
    }

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

    /**
     * Access Token을 블랙리스트에 추가합니다.
     *
     * @param accessToken 블랙리스트에 추가할 Access Token
     * @throws BusinessException 블랙리스트 추가 실패 시
     */
    public void blacklistAccessToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.error("Access token is null or blank for blacklisting");
            throw new BusinessException(ErrorCode.INVALID_TOKEN_BLACKLIST_PARAMETERS);
        }
        
        try {
            final String tokenId = jwtTokenProvider.getTokenId(accessToken);
            final long expiry = jwtTokenProvider.getRemainingExpiry(accessToken);
            
            if (tokenId == null || expiry <= 0) {
                log.error("Invalid access token for blacklisting - tokenId: {}, expiry: {}", tokenId, expiry);
                throw new BusinessException(ErrorCode.INVALID_TOKEN_BLACKLIST_PARAMETERS);
            }
            
            addToBlacklist(tokenId, expiry);
        } catch (final BusinessException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to blacklist access token: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_BLACKLIST_OPERATION_FAILED);
        }
    }

    /**
     * Refresh Token을 블랙리스트에 추가합니다.
     *
     * @param refreshToken 블랙리스트에 추가할 Refresh Token
     * @throws BusinessException 블랙리스트 추가 실패 시
     */
    public void blacklistRefreshToken(final String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.error("Refresh token is null or blank for blacklisting");
            throw new BusinessException(ErrorCode.INVALID_TOKEN_BLACKLIST_PARAMETERS);
        }
        
        try {
            final String tokenId = jwtTokenProvider.getTokenId(refreshToken);
            final long expiry = jwtTokenProvider.getRemainingExpiry(refreshToken);
            
            if (tokenId == null || expiry <= 0) {
                log.error("Invalid refresh token for blacklisting - tokenId: {}, expiry: {}", tokenId, expiry);
                throw new BusinessException(ErrorCode.INVALID_TOKEN_BLACKLIST_PARAMETERS);
            }
            
            addToBlacklist(tokenId, expiry);
        } catch (final BusinessException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to blacklist refresh token: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_BLACKLIST_OPERATION_FAILED);
        }
    }
} 