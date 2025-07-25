package com.ururulab.ururu.auth.constants;

/**
 * 인증 관련 상수들을 정의하는 클래스.
 */
public final class AuthConstants {

    private AuthConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    // Spring Security 권한 상수
    public static final String AUTHORITY_ROLE_MEMBER = "ROLE_MEMBER";
    public static final String AUTHORITY_ROLE_SELLER = "ROLE_SELLER";

    // 토큰 관련 상수
    public static final int MAX_REFRESH_TOKENS_PER_USER = 5;
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    // Redis 키 접두사
    public static final String REFRESH_MEMBER_KEY_PREFIX = "refresh:member:";
    public static final String REFRESH_SELLER_KEY_PREFIX = "refresh:seller:";
    public static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    // 기본값
    public static final UserType DEFAULT_USER_TYPE = UserType.MEMBER;
    public static final UserRole DEFAULT_ROLE = UserRole.NORMAL;
    
    // OAuth 관련 상수
    public static final String OAUTH_CODE_KEY_PREFIX = "oauth:code:";
    public static final int OAUTH_CODE_TTL_SECONDS = 300; // 5분
    
    // 프론트엔드 URL 관련
    public static final String FRONTEND_BASE_URL_PROD = "https://www.ururu.shop";
    public static final String FRONTEND_BASE_URL_DEV = "http://localhost:3000";
    
    // 로깅 관련
    public static final int SENSITIVE_DATA_PREVIEW_LENGTH = 8;
    public static final String MASKED_DATA_PLACEHOLDER = "***";
} 