package com.ururulab.ururu.auth.jwt;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 컴포넌트.
 * 
 * <p>액세스 토큰과 리프레시 토큰의 생성, 검증, 파싱을 처리합니다.
 * 토큰의 구조 검증, 만료 확인, 클레임 추출 등의 기능을 제공합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_JTI = "jti";

    private final JwtProperties jwtProperties;

    /**
     * 액세스 토큰을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param role 사용자 역할
     * @param userType 사용자 타입
     * @return 생성된 액세스 토큰
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public String generateAccessToken(final Long userId, final String email, final UserRole role, final UserType userType) {
        validateAccessTokenParameters(userId, email, role, userType);
        return createToken(userId, email, role.getValue(), userType.getValue(), TokenType.ACCESS, jwtProperties.getAccessTokenExpiry());
    }

    /**
     * 리프레시 토큰을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     * @return 생성된 리프레시 토큰
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public String generateRefreshToken(final Long userId, final UserType userType) {
        validateRefreshTokenParameters(userId, userType);
        return createToken(userId, null, null, userType.getValue(), TokenType.REFRESH, jwtProperties.getRefreshTokenExpiry());
    }

    /**
     * 토큰에서 사용자 ID를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public Long getMemberId(final String token) {
        final Claims claims = parseToken(token);
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    /**
     * 토큰에서 이메일을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getEmail(final String token) {
        final Claims claims = parseToken(token);
        return claims.get(CLAIM_EMAIL, String.class);
    }

    /**
     * 토큰에서 역할을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 역할
     */
    public String getRole(final String token) {
        final Claims claims = parseToken(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    /**
     * 토큰에서 사용자 타입을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 타입
     */
    public String getUserType(final String token) {
        final Claims claims = parseToken(token);
        return claims.get(CLAIM_USER_TYPE, String.class);
    }

    /**
     * 토큰에서 사용자 타입을 enum으로 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 타입 enum
     */
    public UserType getUserTypeAsEnum(final String token) {
        final String userTypeString = getUserType(token);
        return userTypeString != null ? UserType.fromString(userTypeString) : null;
    }

    /**
     * 토큰에서 역할을 enum으로 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 역할 enum
     */
    public UserRole getRoleAsEnum(final String token) {
        final String roleString = getRole(token);
        return roleString != null ? UserRole.fromString(roleString) : null;
    }

    /**
     * 토큰의 유효성을 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(final String token) {
        try {
            parseToken(token);
            return true;
        } catch (final JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isTokenExpired(final String token) {
        try {
            final Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (final ExpiredJwtException e) {
            return true;
        } catch (final JwtException e) {
            log.debug("JWT parsing failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    /**
     * 액세스 토큰의 만료 시간을 반환합니다.
     *
     * @return 액세스 토큰 만료 시간 (초)
     */
    public Long getAccessTokenExpiry() {
        return jwtProperties.getAccessTokenExpiry();
    }

    /**
     * 리프레시 토큰의 만료 시간을 반환합니다.
     *
     * @return 리프레시 토큰 만료 시간 (초)
     */
    public long getRefreshTokenExpirySeconds() {
        return jwtProperties.getRefreshTokenExpiry();
    }

    /**
     * 토큰이 리프레시 토큰인지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return 리프레시 토큰이면 true, 아니면 false
     */
    public boolean isRefreshToken(final String token) {
        final Claims claims = parseToken(token);
        return TokenType.REFRESH.name().equals(claims.get(CLAIM_TYPE, String.class));
    }

    /**
     * 토큰이 액세스 토큰인지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return 액세스 토큰이면 true, 아니면 false
     */
    public boolean isAccessToken(final String token) {
        final Claims claims = parseToken(token);
        return TokenType.ACCESS.name().equals(claims.get(CLAIM_TYPE, String.class));
    }

    /**
     * 토큰의 고유 ID를 반환합니다.
     *
     * @param token JWT 토큰
     * @return 토큰 고유 ID
     */
    public String getTokenId(final String token) {
        final Claims claims = parseToken(token);
        return claims.getId();
    }

    /**
     * 토큰의 남은 만료 시간을 반환합니다.
     *
     * @param token JWT 토큰
     * @return 남은 만료 시간 (초)
     */
    public long getRemainingExpiry(final String token) {
        final Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        return Math.max((expiration.getTime() - now) / 1000, 0);
    }

    // ==================== Private Helper Methods ====================

    /**
     * 액세스 토큰 생성 파라미터를 검증합니다.
     */
    private void validateAccessTokenParameters(final Long userId, final String email, final UserRole role, final UserType userType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (userType == null) {
            throw new IllegalArgumentException("User type cannot be null");
        }
    }

    /**
     * 리프레시 토큰 생성 파라미터를 검증합니다.
     */
    private void validateRefreshTokenParameters(final Long userId, final UserType userType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (userType == null) {
            throw new IllegalArgumentException("User type cannot be null");
        }
    }

    /**
     * JWT 토큰을 생성합니다.
     */
    private String createToken(final Long userId, final String email, final String role, final String userType,
                               final TokenType type, final long expirySeconds) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + (expirySeconds * 1000));

        final JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, type.name())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .signWith(getSecretKey());

        addOptionalClaims(builder, email, role, userType);

        return builder.compact();
    }

    /**
     * 선택적 클레임을 추가합니다.
     */
    private void addOptionalClaims(final JwtBuilder builder, final String email, final String role, final String userType) {
        if (email != null) {
            builder.claim(CLAIM_EMAIL, email);
        }
        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }
        if (userType != null) {
            builder.claim(CLAIM_USER_TYPE, userType);
        }
    }

    /**
     * JWT 토큰을 파싱합니다.
     */
    private Claims parseToken(final String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 비밀키를 생성합니다.
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 토큰 타입 enum.
     */
    public enum TokenType {
        ACCESS, REFRESH
    }
}
