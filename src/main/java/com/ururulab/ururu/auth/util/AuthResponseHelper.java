package com.ururulab.ururu.auth.util;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.service.SecurityLoggingService;

/**
 * 인증 응답 생성을 위한 공통 헬퍼 클래스.
 * 토큰 마스킹과 응답 생성을 중앙화합니다.
 */
public final class AuthResponseHelper {

    private AuthResponseHelper() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 보안을 위해 토큰 정보를 마스킹한 응답 생성
     *
     * @param original 원본 응답
     * @param securityLoggingService 보안 로깅 서비스
     * @return 마스킹된 응답
     */
    public static SocialLoginResponse createSecureResponse(
            final SocialLoginResponse original,
            final SecurityLoggingService securityLoggingService
    ) {
        return SocialLoginResponse.of(
                securityLoggingService.maskToken(original.accessToken()),
                original.refreshToken() != null ? securityLoggingService.maskToken(original.refreshToken()) : null,
                original.expiresIn(),
                original.memberInfo()
        );
    }
} 