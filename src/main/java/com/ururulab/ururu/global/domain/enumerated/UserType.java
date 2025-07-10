package com.ururulab.ururu.global.domain.enumerated;

/**
 * 사용자 타입을 구분하는 enum.
 * JWT 토큰과 Redis 키에서 사용자 타입을 명확히 구분하기 위해 사용됩니다.
 */
public enum UserType {
    MEMBER,    // 일반 회원 (소셜 로그인)
    SELLER     // 판매자 (이메일+비밀번호 로그인)
} 