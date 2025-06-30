package com.ururulab.ururu.auth.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.service.MemberService;

/**
 * 소셜 로그인 서비스의 공통 기능을 제공하는 추상 클래스.
 */
public abstract class AbstractSocialLoginService {

    protected final JwtTokenProvider jwtTokenProvider;
    protected final JwtProperties jwtProperties;
    protected final ObjectMapper objectMapper;
    protected final MemberService memberService;

    protected AbstractSocialLoginService(
            final JwtTokenProvider jwtTokenProvider,
            final JwtProperties jwtProperties,
            final ObjectMapper objectMapper,
            final MemberService memberService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.memberService = memberService;
    }

    protected final SocialLoginResponse createLoginResponse(final Member member) {
        final String jwtAccessToken = jwtTokenProvider.generateAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );
        final String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        final SocialLoginResponse.MemberInfo memberInfo = SocialLoginResponse.MemberInfo.of(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImage()
        );

        return SocialLoginResponse.of(
                jwtAccessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiry(),
                memberInfo
        );
    }

    protected final void validateCode(final String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("인증 코드는 필수입니다.");
        }
    }

    protected final void validateAccessToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("액세스 토큰은 필수입니다.");
        }
    }

    protected static String getTextSafely(final JsonNode node, final String fieldName) {
        final JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }
}