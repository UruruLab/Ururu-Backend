package com.ururulab.ururu.auth.service.social;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.oauth.KakaoOAuthProperties;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KakaoLoginServiceTest {
    @Mock
    private KakaoOAuthProperties kakaoOAuthProperties;
    @Mock
    private AccessTokenGenerator accessTokenGenerator;
    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;
    @Mock
    private RestClient socialLoginRestClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MemberService memberService;
    @Mock
    private JwtRefreshService jwtRefreshService;

    private KakaoLoginService kakaoLoginService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // RestClient 체이닝 최소 모킹
        when(socialLoginRestClient.post()).thenReturn(requestBodyUriSpec);
        when(socialLoginRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any(MediaType.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.header(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec); // ★ 추가
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        kakaoLoginService = new KakaoLoginService(
            kakaoOAuthProperties,
            accessTokenGenerator,
            refreshTokenGenerator,
            socialLoginRestClient,
            objectMapper,
            memberService,
            jwtRefreshService
        );
    }

    @Test
    @DisplayName("정상적으로 카카오 로그인 성공")
    void processLogin_success() throws Exception {
        String code = "valid_code";
        String accessToken = "access_token_value";
        String tokenResponse = "{\"access_token\":\"access_token_value\"}";
        String memberInfoResponse = "{\"id\":\"123\",\"kakao_account\":{\"email\":\"test@kakao.com\",\"profile\":{\"nickname\":\"닉네임\",\"profile_image_url\":\"img_url\"}}}";
        JsonNode tokenJson = mock(JsonNode.class);
        JsonNode memberInfoJson = mock(JsonNode.class);
        JsonNode kakaoAccountJson = mock(JsonNode.class);
        JsonNode profileJson = mock(JsonNode.class);
        Member member = mock(Member.class);
        SocialMemberInfo socialMemberInfo = SocialMemberInfo.of("123", "test@kakao.com", "닉네임", "img_url", SocialProvider.KAKAO);
        SocialLoginResponse loginResponse = mock(SocialLoginResponse.class);

        when(kakaoOAuthProperties.buildTokenRequestBody(code)).thenReturn("body");
        when(kakaoOAuthProperties.getTokenUri()).thenReturn("token_uri");
        when(responseSpec.body(String.class)).thenReturn(tokenResponse, memberInfoResponse);
        when(objectMapper.readTree(tokenResponse)).thenReturn(tokenJson);
        when(tokenJson.get("access_token")).thenReturn(mock(JsonNode.class));
        when(tokenJson.get("access_token").asText()).thenReturn(accessToken);
        when(objectMapper.readTree(memberInfoResponse)).thenReturn(memberInfoJson);
        when(memberInfoJson.get("kakao_account")).thenReturn(kakaoAccountJson);
        when(kakaoAccountJson.get("profile")).thenReturn(profileJson);
        when(memberInfoJson.get("id")).thenReturn(mock(JsonNode.class));
        when(memberInfoJson.get("id").asText()).thenReturn("123");
        when(kakaoAccountJson.get("email")).thenReturn(mock(JsonNode.class));
        when(kakaoAccountJson.get("email").asText()).thenReturn("test@kakao.com");
        when(profileJson.get("nickname")).thenReturn(mock(JsonNode.class));
        when(profileJson.get("nickname").asText()).thenReturn("닉네임");
        when(profileJson.get("profile_image_url")).thenReturn(mock(JsonNode.class));
        when(profileJson.get("profile_image_url").asText()).thenReturn("img_url");
        when(memberService.findOrCreateMember(any())).thenReturn(member);
        when(accessTokenGenerator.generateAccessToken(any(), any(), any(), any())).thenReturn("jwt");
        when(refreshTokenGenerator.generateRefreshToken(any(), any())).thenReturn("refresh");
        doNothing().when(jwtRefreshService).storeRefreshToken(any(), any(), any());
        when(member.getId()).thenReturn(1L);
        when(member.getEmail()).thenReturn("test@kakao.com");
        when(member.getNickname()).thenReturn("닉네임");
        when(member.getProfileImage()).thenReturn("img_url");
        when(member.getRole()).thenReturn(Role.NORMAL); // Role enum 타입으로 stubbing
        when(accessTokenGenerator.getExpirySeconds()).thenReturn(3600L);
        // 추가: memberInfoUri 모킹 및 체인 모킹
        when(kakaoOAuthProperties.getMemberInfoUri()).thenReturn("memberInfoUri");
        when(requestHeadersUriSpec.uri("memberInfoUri")).thenReturn(requestHeadersUriSpec);

        SocialLoginResponse response = kakaoLoginService.processLogin(code);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("state 파라미터 누락 시 예외 발생")
    void getAuthorizationUrl_missingState_throwsException() {
        assertThatThrownBy(() -> kakaoLoginService.getAuthorizationUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSRF 방지를 위한 state 파라미터는 필수입니다.");
    }

    @Test
    @DisplayName("인증 코드 누락 시 예외 발생")
    void getAccessToken_missingCode_throwsException() {
        assertThatThrownBy(() -> kakaoLoginService.getAccessToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("인증 코드는 필수입니다.");
    }

    @Test
    @DisplayName("토큰 응답 파싱 실패 시 예외 발생")
    void getAccessToken_jsonParseException_throwsException() throws Exception {
        String code = "code";
        when(kakaoOAuthProperties.buildTokenRequestBody(code)).thenReturn("body");
        when(kakaoOAuthProperties.getTokenUri()).thenReturn("token_uri");
        when(responseSpec.body(String.class)).thenReturn("invalid_json");
        when(objectMapper.readTree("invalid_json")).thenThrow(new JsonProcessingException("파싱 실패"){});
        assertThatThrownBy(() -> kakaoLoginService.getAccessToken(code))
                .isInstanceOf(BusinessException.class)
                .hasMessage("소셜 로그인 인증에 실패했습니다.");
    }

    @Test
    @DisplayName("회원 정보 응답 파싱 실패 시 예외 발생")
    void getMemberInfo_jsonParseException_throwsException() throws Exception {
        String accessToken = "access_token";
        when(responseSpec.body(String.class)).thenReturn("invalid_json");
        when(objectMapper.readTree("invalid_json")).thenThrow(new JsonProcessingException("파싱 실패"){});
        assertThatThrownBy(() -> kakaoLoginService.getMemberInfo(accessToken))
                .isInstanceOf(BusinessException.class)
                .hasMessage("회원 정보를 가져올 수 없습니다.");
    }
} 