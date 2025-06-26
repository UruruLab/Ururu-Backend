package com.ururulab.ururu.auth.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.exception.SocialMemberInfoException;
import com.ururulab.ururu.auth.exception.SocialTokenExchangeException;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.oauth.KakaoOAuthProperties;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.service.SocialLoginService;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 카카오 소셜 로그인 서비스.
 * RestClient를 사용한 동기적 HTTP 통신으로 리팩토링됨.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginService implements SocialLoginService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int SENSITIVE_DATA_PREVIEW_LENGTH = 10;
    private static final String MASKED_DATA_PLACEHOLDER = "***";

    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final JwtRefreshService jwtRefreshService;
    private final RestClient restClient; // WebClient → RestClient 변경
    private final ObjectMapper objectMapper;
    private final MemberTransactionService memberTransactionService;

    @Override
    public String getAuthorizationUrl(final String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("CSRF 방지를 위한 state 파라미터는 필수입니다.");
        }
        return kakaoOAuthProperties.buildAuthorizationUrl(state);
    }

    @Override
    public String getAccessToken(final String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("인증 코드는 필수입니다.");
        }

        try {
            final String requestBody = kakaoOAuthProperties.buildTokenRequestBody(code);

            log.debug("Requesting access token from Kakao with code: {}",
                    maskSensitiveData(code));

            // RestClient 동기 호출 (.block() 제거)
            final String response = restClient.post()
                    .uri(kakaoOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return extractAccessTokenFromResponse(response);

        } catch (final RestClientException e) { // WebClientException → RestClientException
            log.error("Failed to exchange code for access token", e);
            throw new SocialTokenExchangeException("카카오 액세스 토큰 교환에 실패했습니다.", e);
        }
    }

    @Override
    public SocialMemberInfo getMemberInfo(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("액세스 토큰은 필수입니다.");
        }

        try {
            log.debug("Requesting member info from Kakao");

            // RestClient 동기 호출 (.block() 제거)
            final String response = restClient.get()
                    .uri(kakaoOAuthProperties.getUserInfoUri())
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                    .retrieve()
                    .body(String.class);

            return parseMemberInfoFromResponse(response);

        } catch (final RestClientException e) { // WebClientException → RestClientException
            log.error("Failed to retrieve member info from Kakao", e);
            throw new SocialMemberInfoException("카카오 회원 정보 조회에 실패했습니다.", e);
        }
    }

    @Override
    public SocialLoginResponse processLogin(final String code) {
        String kakaoAccessToken = getAccessToken(code);
        SocialMemberInfo socialMemberInfo = getMemberInfo(kakaoAccessToken);
        Member member = memberTransactionService.findOrCreateMember(socialMemberInfo);

        String jwtAccessToken = jwtTokenProvider.generateAccessToken(
                member.getId(), member.getEmail(), member.getRole().name());
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        jwtRefreshService.storeRefreshToken(member.getId(), jwtRefreshToken);

        return SocialLoginResponse.of(
                jwtAccessToken,
                jwtRefreshToken,
                jwtProperties.getAccessTokenExpiry(),
                SocialLoginResponse.MemberInfo.of(
                        member.getId(),
                        member.getEmail(),
                        member.getNickname(),
                        member.getProfileImage()
                )
        );
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    private String extractAccessTokenFromResponse(final String responseBody) {
        try {
            final JsonNode jsonNode = objectMapper.readTree(responseBody);
            final JsonNode accessTokenNode = jsonNode.get("access_token");

            if (accessTokenNode == null || accessTokenNode.isNull()) {
                throw new SocialTokenExchangeException("응답에 액세스 토큰이 없습니다.");
            }

            return accessTokenNode.asText();
        } catch (final Exception e) {
            log.error("Failed to parse access token from response");
            throw new SocialTokenExchangeException("액세스 토큰 파싱에 실패했습니다.", e);
        }
    }

    private SocialMemberInfo parseMemberInfoFromResponse(final String responseBody) {
        try {
            final JsonNode jsonNode = objectMapper.readTree(responseBody);
            @SuppressWarnings("unchecked")
            final Map<String, Object> attributes = objectMapper.convertValue(jsonNode, Map.class);
            return SocialMemberInfo.fromKakaoAttributes(attributes);
        } catch (final Exception e) {
            log.error("Failed to parse member info from response");
            throw new SocialMemberInfoException("회원 정보 파싱에 실패했습니다.", e);
        }
    }

    private String maskSensitiveData(final String data) {
        if (data == null || data.length() <= SENSITIVE_DATA_PREVIEW_LENGTH) {
            return MASKED_DATA_PLACEHOLDER;
        }
        return data.substring(0, SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
    }
}