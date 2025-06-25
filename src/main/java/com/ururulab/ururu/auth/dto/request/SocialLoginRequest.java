package com.ururulab.ururu.auth.dto.request;

import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 소셜 로그인 요청 DTO.
 *
 * <p>클라이언트로부터 받은 소셜 로그인 요청을 표준화된 형태로 처리합니다.</p>
 */
public record SocialLoginRequest(
        @NotNull(message = "소셜 제공자는 필수입니다")
        SocialProvider provider,

        @NotBlank(message = "인증 코드는 필수입니다")
        String code,

        String state // null 가능, CSRF 방지용 상태값 (선택적)
) {

    /**
     * SocialLoginRequest 생성을 위한 정적 팩토리 메서드.
     *
     * @param provider 소셜 제공자
     * @param code 인증 코드
     * @param state CSRF 방지용 상태값 (null 가능)
     * @return 검증된 SocialLoginRequest 객체
     */
    public static SocialLoginRequest of(
            final SocialProvider provider,
            final String code,
            final String state
    ) {
        return new SocialLoginRequest(provider, code, state);
    }

    /**
     * state 없이 요청 생성 (일부 제공자에서 선택적).
     *
     * @param provider 소셜 제공자
     * @param code 인증 코드
     * @return state가 null인 SocialLoginRequest 객체
     */
    public static SocialLoginRequest withoutState(
            final SocialProvider provider,
            final String code
    ) {
        return of(provider, code, null);
    }
}