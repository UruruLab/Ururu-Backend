package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰 검증 서비스.
 * 
 * <p>JWT 토큰의 유효성 검사, 사용자 정보 추출, 블랙리스트 확인 등을 통합 관리합니다.
 * 액세스 토큰과 리프레시 토큰에 대한 검증 로직을 제공하며, 보안을 위한 다층 검증을 수행합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class TokenValidator {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistStorage tokenBlacklistStorage;

    /**
     * 액세스 토큰을 검증하고 사용자 정보를 추출합니다.
     *
     * @param accessToken 검증할 액세스 토큰
     * @return 토큰 검증 결과 (성공 시 사용자 정보 포함)
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public TokenValidationResult validateAccessToken(final String accessToken) {
        validateTokenNotNull(accessToken, "Access token");
        
        // 1. JWT 토큰 구조 및 서명 검증
        validateTokenStructure(accessToken);
        
        // 2. 토큰 타입 확인 (액세스 토큰인지)
        validateTokenType(accessToken, true);
        
        // 3. 토큰 만료 확인
        validateTokenExpiration(accessToken);
        
        // 4. 블랙리스트 확인
        validateTokenBlacklist(accessToken);
        
        // 5. 사용자 정보 추출
        final TokenValidationResult result = extractTokenInfo(accessToken);
        
        log.debug("Access token validation successful for user: {} (type: {})", 
                result.userId(), result.userType());
        
        return result;
    }

    /**
     * 리프레시 토큰을 검증하고 사용자 정보를 추출합니다.
     *
     * @param refreshToken 검증할 리프레시 토큰
     * @return 토큰 검증 결과 (성공 시 사용자 정보 포함)
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public TokenValidationResult validateRefreshToken(final String refreshToken) {
        validateTokenNotNull(refreshToken, "Refresh token");
        
        // 1. JWT 토큰 구조 및 서명 검증
        validateTokenStructure(refreshToken);
        
        // 2. 토큰 타입 확인 (리프레시 토큰인지)
        validateTokenType(refreshToken, false);
        
        // 3. 토큰 만료 확인
        validateTokenExpiration(refreshToken);
        
        // 4. 블랙리스트 확인
        validateTokenBlacklist(refreshToken);
        
        // 5. 사용자 정보 추출
        final TokenValidationResult result = extractTokenInfo(refreshToken);
        
        log.debug("Refresh token validation successful for user: {} (type: {})", 
                result.userId(), result.userType());
        
        return result;
    }

    // ==================== Private Helper Methods ====================

    /**
     * 토큰이 null이거나 비어있는지 검증합니다.
     */
    private void validateTokenNotNull(final String token, final String tokenType) {
        if (token == null || token.isBlank()) {
            final ErrorCode errorCode = "Refresh token".equals(tokenType) 
                ? ErrorCode.MISSING_REFRESH_TOKEN 
                : ErrorCode.MISSING_AUTHORIZATION_HEADER;
            throw new BusinessException(errorCode);
        }
    }

    /**
     * JWT 토큰의 구조와 서명을 검증합니다.
     */
    private void validateTokenStructure(final String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    /**
     * 토큰 타입을 검증합니다.
     */
    private void validateTokenType(final String token, final boolean isAccessToken) {
        final boolean isValidType = isAccessToken 
            ? jwtTokenProvider.isAccessToken(token)
            : jwtTokenProvider.isRefreshToken(token);
            
        if (!isValidType) {
            final ErrorCode errorCode = isAccessToken 
                ? ErrorCode.INVALID_JWT_TOKEN 
                : ErrorCode.INVALID_REFRESH_TOKEN;
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 토큰 만료 여부를 검증합니다.
     */
    private void validateTokenExpiration(final String token) {
        if (jwtTokenProvider.isTokenExpired(token)) {
            throw new BusinessException(ErrorCode.EXPIRED_JWT_TOKEN);
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 검증합니다.
     */
    private void validateTokenBlacklist(final String token) {
        final String tokenId = jwtTokenProvider.getTokenId(token);
        if (tokenBlacklistStorage.isTokenBlacklisted(tokenId)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    /**
     * 토큰에서 사용자 정보를 추출합니다.
     */
    private TokenValidationResult extractTokenInfo(final String token) {
        final Long userId = jwtTokenProvider.getMemberId(token);
        final String userType = jwtTokenProvider.getUserType(token);
        final String tokenId = jwtTokenProvider.getTokenId(token);
        
        return TokenValidationResult.of(userId, userType, tokenId);
    }

    /**
     * 토큰 검증 결과를 담는 불변 객체.
     * 
     * @param userId 사용자 ID
     * @param userType 사용자 타입 (MEMBER/SELLER)
     * @param tokenId 토큰 고유 ID
     */
    public record TokenValidationResult(Long userId, String userType, String tokenId) {
        
        /**
         * TokenValidationResult를 생성합니다.
         */
        public static TokenValidationResult of(final Long userId, final String userType, final String tokenId) {
            return new TokenValidationResult(userId, userType, tokenId);
        }
    }
} 