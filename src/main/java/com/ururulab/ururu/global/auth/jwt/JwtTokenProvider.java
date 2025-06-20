package com.ururulab.ururu.global.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 컴포넌트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(final Long memberId, final String email, final String role) {
        return createToken(memberId, email, role, TokenType.ACCESS, jwtProperties.getAccessTokenExpiry());
    }

    public String generateRefreshToken(final Long memberId) {
        return createToken(memberId, null, null, TokenType.REFRESH, jwtProperties.getRefreshTokenExpiry());
    }

    public Long getMemberId(final String token) {
        final Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    public String getEmail(final String token) {
        final Claims claims = parseToken(token);
        return claims.get(CLAIM_EMAIL, String.class);
    }

    public String getRole(final String token) {
        final Claims claims = parseToken(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    public boolean validateToken(final String token) {
        try {
            parseToken(token);
            return true;
        } catch (final JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(final String token) {
        try {
            final Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (final JwtException e) {
            return true;
        }
    }

    private String createToken(final Long memberId, final String email, final String role,
                               final TokenType type, final long expirySeconds) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + expirySeconds * 1000);

        final JwtBuilder builder = Jwts.builder()
                .subject(memberId.toString())
                .claim(CLAIM_TYPE, type.name())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSecretKey());

        if (email != null) {
            builder.claim(CLAIM_EMAIL, email);
        }
        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }

        return builder.compact();
    }

    private Claims parseToken(final String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public enum TokenType {
        ACCESS, REFRESH
    }
}