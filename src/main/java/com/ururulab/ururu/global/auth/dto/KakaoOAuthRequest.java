package com.ururulab.ururu.global.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 OAuth 로그인 요청 DTO.
 */
public record KakaoOAuthRequest(
        @NotBlank(message = "인증 코드는 필수입니다")
        String code,

        String state
) {
    public static KakaoOAuthRequest of(final String code, final String state) {
        return new KakaoOAuthRequest(code, state);
    }
}