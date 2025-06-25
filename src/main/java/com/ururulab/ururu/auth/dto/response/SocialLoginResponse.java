package com.ururulab.ururu.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 소셜 로그인 응답 DTO.
 *
 * <p>소셜 로그인 성공 시 JWT 토큰과 회원 정보를 포함한 응답을 제공합니다.</p>
 */
public record SocialLoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("member_info") MemberInfo memberInfo
) {

    private static final String BEARER_TYPE = "Bearer";

    /**
     * 완전한 토큰 정보로 소셜 로그인 응답 생성 (이펙티브 자바 아이템 1).
     *
     * @param accessToken JWT 액세스 토큰
     * @param refreshToken JWT 리프레시 토큰 (null 가능)
     * @param expiresIn 토큰 만료 시간 (초)
     * @param memberInfo 회원 정보
     * @return 소셜 로그인 응답
     * @throws IllegalArgumentException 필수 파라미터가 유효하지 않은 경우
     */
    public static SocialLoginResponse of(
            final String accessToken,
            final String refreshToken,
            final Long expiresIn,
            final MemberInfo memberInfo
    ) {
        validateAccessToken(accessToken);
        validateExpiresIn(expiresIn);
        validateMemberInfo(memberInfo);

        return new SocialLoginResponse(accessToken, refreshToken, BEARER_TYPE, expiresIn, memberInfo);
    }

    /**
     * 액세스 토큰만으로 응답 생성 (리프레시 토큰 없는 경우).
     *
     * @param accessToken JWT 액세스 토큰
     * @param expiresIn 토큰 만료 시간 (초)
     * @param memberInfo 회원 정보
     * @return 액세스 토큰만 포함된 소셜 로그인 응답
     */
    public static SocialLoginResponse withAccessTokenOnly(
            final String accessToken,
            final Long expiresIn,
            final MemberInfo memberInfo
    ) {
        return of(accessToken, null, expiresIn, memberInfo);
    }

    private static void validateAccessToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("액세스 토큰은 필수입니다.");
        }
    }

    private static void validateExpiresIn(final Long expiresIn) {
        if (expiresIn == null || expiresIn <= 0) {
            throw new IllegalArgumentException("만료 시간은 0보다 커야 합니다.");
        }
    }

    private static void validateMemberInfo(final MemberInfo memberInfo) {
        if (memberInfo == null) {
            throw new IllegalArgumentException("회원 정보는 필수입니다.");
        }
    }

    /**
     * 응답에 포함될 회원 정보 DTO (user → member 변경 반영).
     */
    public record MemberInfo(
            @JsonProperty("member_id") Long memberId,
            String email,
            String nickname,
            @JsonProperty("profile_image") String profileImage
    ) {

        /**
         * 완전한 회원 정보로 MemberInfo 생성.
         *
         * @param memberId 회원 ID
         * @param email 이메일 (null 가능)
         * @param nickname 닉네임 (null 가능)
         * @param profileImage 프로필 이미지 URL (null 가능)
         * @return MemberInfo 객체
         * @throws IllegalArgumentException memberId가 null인 경우
         */
        public static MemberInfo of(
                final Long memberId,
                final String email,
                final String nickname,
                final String profileImage
        ) {
            if (memberId == null) {
                throw new IllegalArgumentException("회원 ID는 필수입니다.");
            }
            return new MemberInfo(memberId, email, nickname, profileImage);
        }

        /**
         * 최소 정보로 MemberInfo 생성 (memberId만 필수).
         *
         * @param memberId 회원 ID
         * @return 최소 정보만 포함된 MemberInfo 객체
         */
        public static MemberInfo withMinimalInfo(final Long memberId) {
            return of(memberId, null, null, null);
        }
    }
}