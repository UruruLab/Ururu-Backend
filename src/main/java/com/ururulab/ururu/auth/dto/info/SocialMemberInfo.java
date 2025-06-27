package com.ururulab.ururu.auth.dto.info;

import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;

/**
 * 소셜 제공자별 회원 정보를 통일된 형태로 표현하는 DTO.
 *
 * <p>각 소셜 제공자의 응답 형식을 표준화하여 일관된 회원 정보 처리가 가능하도록 합니다.</p>
 */
public record SocialMemberInfo(
        String socialId,
        String email,
        String nickname,
        String profileImage,
        SocialProvider provider
) {

    /**
     * 완전한 회원 정보로 SocialMemberInfo 생성 (이펙티브 자바 아이템 1).
     *
     * @param socialId 소셜 제공자 고유 ID
     * @param email 회원 이메일 (null 가능, 제공자가 이메일을 제공하지 않는 경우)
     * @param nickname 회원 닉네임 (null 가능, 제공자가 닉네임을 제공하지 않는 경우)
     * @param profileImage 프로필 이미지 URL (null 가능, 이미지가 없는 경우)
     * @param provider 소셜 제공자
     * @return 검증된 SocialMemberInfo 객체
     * @throws IllegalArgumentException socialId 또는 provider가 null인 경우
     */
    public static SocialMemberInfo of(
            final String socialId,
            final String email,
            final String nickname,
            final String profileImage,
            final SocialProvider provider
    ) {
        validateRequiredFields(socialId, provider);
        return new SocialMemberInfo(socialId, email, nickname, profileImage, provider);
    }

    /**
     * 최소 정보로 SocialMemberInfo 생성 (socialId와 provider만 필수).
     *
     * @param socialId 소셜 제공자 고유 ID
     * @param provider 소셜 제공자
     * @return 최소 정보만 포함된 SocialMemberInfo 객체
     */
    public static SocialMemberInfo withMinimalInfo(
            final String socialId,
            final SocialProvider provider
    ) {
        return of(socialId, null, null, null, provider);
    }

    /**
     * 카카오 API 응답으로부터 SocialMemberInfo 생성.
     *
     * @param attributes 카카오 API 응답 Map
     * @return 카카오 회원 정보가 파싱된 SocialMemberInfo 객체
     * @throws IllegalArgumentException 필수 정보가 누락된 경우
     */
    public static SocialMemberInfo fromKakaoAttributes(final java.util.Map<String, Object> attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("카카오 회원 정보가 없습니다");
        }

        final Object idObj = attributes.get("id");
        if (idObj == null) {
            throw new IllegalArgumentException("카카오 회원 ID가 없습니다");
        }
        final String socialId = String.valueOf(idObj);

        @SuppressWarnings("unchecked")
        final java.util.Map<String, Object> kakaoAccount =
                (java.util.Map<String, Object>) attributes.get("kakao_account");
        final String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        @SuppressWarnings("unchecked")
        final java.util.Map<String, Object> profile = kakaoAccount != null ?
                (java.util.Map<String, Object>) kakaoAccount.get("profile") : null;
        final String nickname = profile != null ? (String) profile.get("nickname") : null;
        final String profileImage = profile != null ? (String) profile.get("profile_image_url") : null;

        return of(socialId, email, nickname, profileImage, SocialProvider.KAKAO);
    }

    /**
     * 구글 API 응답으로부터 SocialMemberInfo 생성.
     *
     * @param attributes 구글 API 응답 Map
     * @return 구글 회원 정보가 파싱된 SocialMemberInfo 객체
     * @throws IllegalArgumentException 필수 정보가 누락된 경우
     */
    public static SocialMemberInfo fromGoogleAttributes(final java.util.Map<String, Object> attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("구글 회원 정보가 없습니다");
        }

        final Object idObj = attributes.get("id");
        if (idObj == null) {
            throw new IllegalArgumentException("구글 회원 ID가 없습니다");
        }
        final String socialId = String.valueOf(idObj);

        final String email = (String) attributes.get("email");
        final String nickname = (String) attributes.get("name");
        final String profileImage = (String) attributes.get("picture");

        return of(socialId, email, nickname, profileImage, SocialProvider.GOOGLE);
    }

    private static void validateRequiredFields(final String socialId, final SocialProvider provider) {
        if (socialId == null || socialId.isBlank()) {
            throw new IllegalArgumentException("소셜 ID는 필수입니다.");
        }
        if (provider == null) {
            throw new IllegalArgumentException("소셜 제공자는 필수입니다.");
        }
    }
}