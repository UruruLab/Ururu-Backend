package com.ururulab.ururu.global.auth.service.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.global.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.global.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.global.auth.exception.SocialMemberInfoException;
import com.ururulab.ururu.global.auth.exception.SocialTokenExchangeException;
import com.ururulab.ururu.global.auth.jwt.JwtProperties;
import com.ururulab.ururu.global.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.global.auth.oauth.KakaoOAuthProperties;
import com.ururulab.ururu.global.auth.service.SocialLoginService;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

/**
 * 카카오 소셜 로그인 서비스.
 *
 * <p>카카오 OAuth 2.0 플로우를 처리하며, 기존 프로젝트 구조에 맞춰 구현되었습니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLoginService implements SocialLoginService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final int SENSITIVE_DATA_PREVIEW_LENGTH = 10;
    private static final String MASKED_DATA_PLACEHOLDER = "***";

    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final MemberRepository memberRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

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

            final String response = webClient.post()
                    .uri(kakaoOAuthProperties.getTokenUri())
                    .header("Content-Type", CONTENT_TYPE_FORM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractAccessTokenFromResponse(response);

        } catch (final Exception e) {
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

            final String response = webClient.get()
                    .uri(kakaoOAuthProperties.getUserInfoUri())
                    .header("Authorization", BEARER_PREFIX + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseMemberInfoFromResponse(response);

        } catch (final Exception e) {
            log.error("Failed to retrieve member info from Kakao", e);
            throw new SocialMemberInfoException("카카오 회원 정보 조회에 실패했습니다.", e);
        }
    }

    @Override
    @Transactional
    public SocialLoginResponse processLogin(final String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("인증 코드는 필수입니다.");
        }

        final String accessToken = getAccessToken(code);

        final SocialMemberInfo socialMemberInfo = getMemberInfo(accessToken);

        final Member member = findOrCreateMember(socialMemberInfo);

        final String jwtAccessToken = jwtTokenProvider.generateAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );
        final String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        return SocialLoginResponse.of(
                jwtAccessToken,
                jwtRefreshToken,
                jwtProperties.getAccessTokenExpiry(), // 기존 설정 활용
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

            // 기존 SocialMemberInfo.fromKakaoAttributes 메서드 활용
            return SocialMemberInfo.fromKakaoAttributes(attributes);
        } catch (final Exception e) {
            log.error("Failed to parse member info from response");
            throw new SocialMemberInfoException("회원 정보 파싱에 실패했습니다.", e);
        }
    }

    /**
     * 회원 조회 또는 신규 생성.
     *
     * <p>구매자 전용 소셜 로그인이므로 NORMAL 권한으로 생성합니다.</p>
     */
    private Member findOrCreateMember(final SocialMemberInfo socialMemberInfo) {
        final Optional<Member> existingMember = memberRepository
                .findBySocialProviderAndSocialId(
                        socialMemberInfo.provider(),
                        socialMemberInfo.socialId()
                );

        if (existingMember.isPresent()) {
            final Member member = existingMember.get();
            log.debug("Existing member found: {}", member.getId());
            return member;
        }

        final Member newMember = Member.of(
                socialMemberInfo.nickname(),
                socialMemberInfo.email(),
                socialMemberInfo.provider(),
                socialMemberInfo.socialId(),
                null, // gender - 카카오에서 제공하지 않음
                null, // birth - 카카오에서 제공하지 않음
                null, // phone - 카카오에서 제공하지 않음
                socialMemberInfo.profileImage(),
                Role.NORMAL // 구매자는 NORMAL 권한 (API 명세 준수)
        );

        final Member savedMember = memberRepository.save(newMember);
        log.info("New buyer member created via Kakao: {}", savedMember.getId());

        return savedMember;
    }

    private String maskSensitiveData(final String data) {
        if (data == null || data.length() <= SENSITIVE_DATA_PREVIEW_LENGTH) {
            return MASKED_DATA_PLACEHOLDER;
        }
        return data.substring(0, SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
    }
}