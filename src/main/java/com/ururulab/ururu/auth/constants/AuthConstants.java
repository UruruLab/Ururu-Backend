package com.ururulab.ururu.auth.constants;

/**
 * 인증 관련 상수들을 정의하는 클래스.
 */
public final class AuthConstants {

    private AuthConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    // Spring Security 권한 상수

    // 토큰 관련 상수
    public static final int MAX_REFRESH_TOKENS_PER_USER = 5;
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    // Redis 키 접두사
    public static final String REFRESH_MEMBER_KEY_PREFIX = "refresh:member:";
    public static final String REFRESH_SELLER_KEY_PREFIX = "refresh:seller:";
    public static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    public static final String USER_REVOCATION_KEY_PREFIX = "revoked:user:";

    // OAuth 관련 상수
    
    // 로깅 관련
    public static final String MASKED_DATA_PLACEHOLDER = "***";
}
