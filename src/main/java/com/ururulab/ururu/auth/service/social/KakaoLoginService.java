package com.ururulab.ururu.auth.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.oauth.KakaoOAuthProperties;
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
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Service
public final class KakaoLoginService extends AbstractSocialLoginService implements SocialLoginService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final RestClient socialLoginRestClient;

    public KakaoLoginService(
            final KakaoOAuthProperties kakaoOAuthProperties,
            final JwtTokenProvider jwtTokenProvider,
            final JwtProperties jwtProperties,
            @Qualifier("socialLoginRestClient") final RestClient socialLoginRestClient,
            final ObjectMapper objectMapper,
            final MemberService memberService,
            final JwtRefreshService jwtRefreshService,
            final StringRedisTemplate stringRedisTemplate
    ) {
        super(jwtTokenProvider, jwtProperties, objectMapper, memberService, jwtRefreshService, stringRedisTemplate);
        this.kakaoOAuthProperties = kakaoOAuthProperties;
        this.socialLoginRestClient = socialLoginRestClient;
    }

    @Override
    public String getAuthorizationUrl(final String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("CSRF 방지를 위한 state 파라미터는 필수입니다.");
        }
        return kakaoOAuthProperties.buildAuthorizationUrl(state);
    }

    @Override
    public String getAccessToken(final String code) {
        validateCode(code);

        try {
            final String requestBody = kakaoOAuthProperties.buildTokenRequestBody(code);
            log.debug("카카오 토큰 요청 - URI: {}, Body: {}", kakaoOAuthProperties.getTokenUri());

            final String response = socialLoginRestClient.post()
                    .uri(kakaoOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        final String errorMsg = String.format("카카오 토큰 요청 실패: %s", res.getStatusCode());
                        log.error("{} - URI: {}, RequestBody: {}", errorMsg, kakaoOAuthProperties.getTokenUri(), requestBody);
                        throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
                    })
                    .body(String.class);

            log.debug("카카오 토큰 응답 수신 완료 (토큰 정보는 보안상 마스킹됨)");
            return extractAccessToken(response);
        } catch (final BusinessException e) {
            throw e;
        } catch (final Exception e) {
            log.error("카카오 액세스 토큰 요청 중 예외 발생", e);
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
    }

    @Override
    public SocialMemberInfo getMemberInfo(final String accessToken) {
        validateAccessToken(accessToken);

        try {
            log.debug("카카오 회원 정보 요청 - URI: {}", kakaoOAuthProperties.getMemberInfoUri());
                    
            final String response = socialLoginRestClient.get()
                    .uri(kakaoOAuthProperties.getMemberInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        final String errorMsg = String.format("카카오 회원 정보 조회 실패: %s", res.getStatusCode());
                        log.error("{} - URI: {}", errorMsg, kakaoOAuthProperties.getMemberInfoUri());
                        throw new BusinessException(ErrorCode.SOCIAL_MEMBER_INFO_FAILED);
                    })
                    .body(String.class);

            log.debug("카카오 회원 정보 응답 수신 완료 (개인정보는 보안상 마스킹됨)");
            return parseMemberInfo(response);
        } catch (final BusinessException e) {
            throw e;
        } catch (final Exception e) {
            log.error("카카오 회원 정보 조회 중 예외 발생", e);
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
        return SocialProvider.KAKAO;
    }

    private String extractAccessToken(final String response) {
        if (response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        final JsonNode jsonNode = parseJson(response, "카카오 토큰 응답");
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

        final JsonNode root = parseJson(response, "카카오 회원 정보");
        final JsonNode kakaoAccount = root.get("kakao_account");
        final JsonNode profile = kakaoAccount != null ? kakaoAccount.get("profile") : null;

        final String socialId = root.get("id").asText();
        final String email = kakaoAccount != null ? getTextSafely(kakaoAccount, "email") : null;
        final String nickname = profile != null ? getTextSafely(profile, "nickname") : null;
        final String profileImage = profile != null ? getTextSafely(profile, "profile_image_url") : null;

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