package com.ururulab.ururu.global.auth.dto;

import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;

import java.util.Map;

/**
 * 카카오 OAuth 사용자 정보 DTO.
 */
public record OAuthUserInfo(
        String socialId,
        String email,
        String nickname,
        String profileImage,
        SocialProvider provider
) {
    public static OAuthUserInfo fromKakaoAttributes(final Map<String, Object> attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("카카오 사용자 정보가 없습니다");
        }
        final Object idObj = attributes.get("id");
        if (idObj == null) {
            throw new IllegalArgumentException("카카오 사용자 ID가 없습니다");
        }
        final String socialId = String.valueOf(idObj);

        @SuppressWarnings("unchecked")
        final Map<String, Object> kakaoAccount = safeCast(attributes.get("kakao_account"), Map.class);

        final String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        @SuppressWarnings("unchecked")
        final Map<String, Object> profile = kakaoAccount != null ?
                safeCast(kakaoAccount.get("profile"), Map.class) : null;

        final String nickname = profile != null ? (String) profile.get("nickname") : null;
        final String profileImage = profile != null ? (String) profile.get("profile_image_url") : null;

        return new OAuthUserInfo(socialId, email, nickname, profileImage, SocialProvider.KAKAO);
    }
    private static <T> T safeCast(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (clazz.isInstance(obj)) {
            throw new IllegalArgumentException("예상하지 못한 카카오 API 응답 형식입니다");
        }
        return clazz.cast(obj);
    }
}