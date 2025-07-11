package com.ururulab.ururu.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 판매자 로그인 요청 DTO.
 */
public record SellerLoginRequest(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {
    public static SellerLoginRequest of(final String email, final String password) {
        return new SellerLoginRequest(email, password);
    }
} 