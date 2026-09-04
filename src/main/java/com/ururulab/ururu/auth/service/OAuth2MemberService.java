package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public final class OAuth2MemberService {

    private final MemberService memberService;
    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final JwtRefreshService jwtRefreshService;

    public SocialLoginResponse login(final String registrationId, final Map<String, Object> attributes) {
        final SocialMemberInfo socialMemberInfo = switch (provider(registrationId)) {
            case GOOGLE -> googleMember(attributes);
            case KAKAO -> kakaoMember(attributes);
        };
        final Member member = memberService.findOrCreateMember(socialMemberInfo);
        final UserRole role = member.getRole() == Role.ADMIN ? UserRole.ADMIN : UserRole.MEMBER;
        final String accessToken = accessTokenGenerator.generateAccessToken(
                member.getId(), member.getEmail(), role, UserType.MEMBER
        );
        final String refreshToken = refreshTokenGenerator.generateRefreshToken(member.getId(), UserType.MEMBER);
        jwtRefreshService.storeRefreshToken(member.getId(), UserType.MEMBER.getValue(), refreshToken);

        return SocialLoginResponse.of(
                accessToken,
                refreshToken,
                accessTokenGenerator.getExpirySeconds(),
                SocialLoginResponse.MemberInfo.of(
                        member.getId(), member.getEmail(), member.getNickname(),
                        member.getProfileImage(), UserType.MEMBER.getValue()
                )
        );
    }

    private SocialMemberInfo googleMember(final Map<String, Object> attributes) {
        if (!booleanValue(attributes.get("email_verified"))) {
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
        }
        return SocialMemberInfo.of(
                firstText(attributes, "sub", "id"),
                text(attributes, "email"),
                text(attributes, "name"),
                nullableText(attributes, "picture"),
                SocialProvider.GOOGLE
        );
    }

    private SocialMemberInfo kakaoMember(final Map<String, Object> attributes) {
        final Map<?, ?> account = map(attributes.get("kakao_account"));
        final Map<?, ?> profile = map(account.get("profile"));
        if (!booleanValue(account.get("is_email_valid")) || !booleanValue(account.get("is_email_verified"))) {
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
        }
        return SocialMemberInfo.of(
                text(attributes, "id"),
                text(account, "email"),
                text(profile, "nickname"),
                nullableText(profile, "profile_image_url"),
                SocialProvider.KAKAO
        );
    }

    private SocialProvider provider(final String registrationId) {
        try {
            return SocialProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        } catch (final RuntimeException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
        }
    }

    private Map<?, ?> map(final Object value) {
        if (value instanceof Map<?, ?> result) {
            return result;
        }
        throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
    }

    private String firstText(final Map<?, ?> values, final String first, final String second) {
        final String value = nullableText(values, first);
        return value == null ? text(values, second) : value;
    }

    private String text(final Map<?, ?> values, final String key) {
        final String value = nullableText(values, key);
        if (value == null) {
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
        }
        return value;
    }

    private String nullableText(final Map<?, ?> values, final String key) {
        final Object value = values.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private boolean booleanValue(final Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }
}
