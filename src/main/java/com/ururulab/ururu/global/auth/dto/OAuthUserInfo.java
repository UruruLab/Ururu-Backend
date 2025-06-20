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
        final String socialId = String.valueOf(attributes.get("id"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        final String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        @SuppressWarnings("unchecked")
        final Map<String, Object> profile = kakaoAccount != null ?
                (Map<String, Object>) kakaoAccount.get("profile") : null;

        final String nickname = profile != null ? (String) profile.get("nickname") : null;
        final String profileImage = profile != null ? (String) profile.get("profile_image_url") : null;

        return new OAuthUserInfo(socialId, email, nickname, profileImage, SocialProvider.KAKAO);
    }
}