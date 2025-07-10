package com.ururulab.ururu.auth.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.oauth.GoogleOAuthProperties;
import com.ururulab.ururu.auth.service.SocialLoginService;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.ururulab.ururu.auth.service.JwtRefreshService;

@Slf4j
@Service
public final class GoogleLoginService extends AbstractSocialLoginService implements SocialLoginService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GoogleOAuthProperties googleOAuthProperties;
    private final RestClient socialLoginRestClient;

    public GoogleLoginService(
            final GoogleOAuthProperties googleOAuthProperties,
            final JwtTokenProvider jwtTokenProvider,
            final JwtProperties jwtProperties,
            @Qualifier("socialLoginRestClient") final RestClient socialLoginRestClient,
            final ObjectMapper objectMapper,
            final MemberService memberService,
            final JwtRefreshService jwtRefreshService
    ) {
        super(jwtTokenProvider, jwtProperties, objectMapper, memberService, jwtRefreshService);
        this.googleOAuthProperties = googleOAuthProperties;
        this.socialLoginRestClient = socialLoginRestClient;
    }

    @Override
    public String getAuthorizationUrl(final String state) {
        return googleOAuthProperties.buildAuthorizationUrl(state);
    }

    @Override
    public String getAccessToken(final String code) {
        validateCode(code);

        try {
            final String requestBody = googleOAuthProperties.buildTokenRequestBody(code);
            final String response = socialLoginRestClient.post()
                    .uri(googleOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        final String errorMsg = String.format("구글 토큰 요청 실패: %s", res.getStatusCode());
                        log.error("{} - URI: {}", errorMsg, googleOAuthProperties.getTokenUri());
                        throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
                    })
                    .body(String.class);

            return extractAccessToken(response);
        } catch (final BusinessException e) {
            throw e;
        } catch (final Exception e) {
            log.error("구글 액세스 토큰 요청 중 예외 발생", e);
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
    }

    @Override
    public SocialMemberInfo getMemberInfo(final String accessToken) {
        validateAccessToken(accessToken);

        try {
            final String response = socialLoginRestClient.get()
                    .uri(googleOAuthProperties.getMemberInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        final String errorMsg = String.format("구글 회원 정보 조회 실패: %s", res.getStatusCode());
                        log.error("{} - URI: {}", errorMsg, googleOAuthProperties.getMemberInfoUri());
                        throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
                    })
                    .body(String.class);

            return parseMemberInfo(response);
        } catch (final BusinessException e) {
            throw e;
        } catch (final Exception e) {
            log.error("구글 회원 정보 조회 중 예외 발생", e);
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
        }
    }

    @Override
    public SocialLoginResponse processLogin(final String code) {
        final String accessToken = getAccessToken(code);
        final SocialMemberInfo memberInfo = getMemberInfo(accessToken);
        final Member member = memberService.findOrCreateMember(memberInfo);
        return createLoginResponse(member);
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    private String extractAccessToken(final String response) {
        if (response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        final JsonNode jsonNode = parseJson(response, "구글 토큰 응답");
        final JsonNode tokenNode = jsonNode.get("access_token");
        if (tokenNode == null || tokenNode.asText().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
        return tokenNode.asText();
    }

    private SocialMemberInfo parseMemberInfo(final String response) {
        if (response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
        }

        final JsonNode root = parseJson(response, "구글 회원 정보");
        final String socialId = getTextSafely(root, "id");
        final String email = getTextSafely(root, "email");
        final String nickname = getTextSafely(root, "name");
        final String profileImage = getTextSafely(root, "picture");

        return SocialMemberInfo.of(socialId, email, nickname, profileImage, getProvider());
    }

    private JsonNode parseJson(final String response, final String context) {
        try {
            return objectMapper.readTree(response);
        } catch (final Exception e) {
            log.error("JSON 파싱 실패 - {}: {}", context, e.getMessage());
            throw new RuntimeException(context + " 파싱에 실패했습니다.", e);
        }
    }
}