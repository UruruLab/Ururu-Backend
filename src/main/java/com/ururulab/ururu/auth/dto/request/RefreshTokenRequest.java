package com.ururulab.ururu.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO.
 * 클라이언트로부터 받은 Refresh Token을 담는다.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다")
        String refreshToken
) {
    public static RefreshTokenRequest of(final String refreshToken) {
        return new RefreshTokenRequest(refreshToken);
    }
}
