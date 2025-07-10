package com.ururulab.ururu.auth.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;

/**
 * 소셜 로그인 서비스의 공통 기능을 제공하는 추상 클래스.
 */
@Slf4j
public abstract class AbstractSocialLoginService {

    protected final AccessTokenGenerator accessTokenGenerator;
    protected final RefreshTokenGenerator refreshTokenGenerator;
    protected final ObjectMapper objectMapper;
    protected final MemberService memberService;
    protected final JwtRefreshService jwtRefreshService;

    protected AbstractSocialLoginService(
            final AccessTokenGenerator accessTokenGenerator,
            final RefreshTokenGenerator refreshTokenGenerator,
            final ObjectMapper objectMapper,
            final MemberService memberService,
            final JwtRefreshService jwtRefreshService
    ) {
        this.accessTokenGenerator = accessTokenGenerator;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.objectMapper = objectMapper;
        this.memberService = memberService;
        this.jwtRefreshService = jwtRefreshService;
    }

    protected final SocialLoginResponse createLoginResponse(final Member member) {
        final String jwtAccessToken = accessTokenGenerator.generateAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name(),
                AuthConstants.USER_TYPE_MEMBER
        );
        final String refreshToken = refreshTokenGenerator.generateRefreshToken(
                member.getId(), 
                AuthConstants.USER_TYPE_MEMBER
        );

        // Refresh Token을 Redis에 저장
        jwtRefreshService.storeRefreshToken(member.getId(), AuthConstants.USER_TYPE_MEMBER, refreshToken);

        final SocialLoginResponse.MemberInfo memberInfo = SocialLoginResponse.MemberInfo.of(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImage(),
                AuthConstants.USER_TYPE_MEMBER
        );

        return SocialLoginResponse.of(
                jwtAccessToken,
                refreshToken,
                accessTokenGenerator.getExpirySeconds(),
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