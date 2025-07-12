package com.ururulab.ururu.auth.service.social;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.oauth.GoogleOAuthProperties;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleLoginService 테스트")
class GoogleLoginServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private GoogleOAuthProperties googleOAuthProperties;

    @Mock
    private MemberService memberService;

    @Mock
    private AccessTokenGenerator accessTokenGenerator;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private JwtRefreshService jwtRefreshService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GoogleLoginService googleLoginService;

    @Nested
    @DisplayName("getAuthorizationUrl")
    class GetAuthorizationUrlTest {

        @Test
        @DisplayName("정상적으로 인증 URL을 생성한다")
        void getAuthorizationUrl_success() {
            // given
            String state = "test-state";
            String expectedUrl = "https://accounts.google.com/oauth/authorize?client_id=test&redirect_uri=test&response_type=code&scope=email+profile&state=test-state";
            
            when(googleOAuthProperties.buildAuthorizationUrl(state)).thenReturn(expectedUrl);

            // when
            String result = googleLoginService.getAuthorizationUrl(state);

            // then
            assertThat(result).isEqualTo(expectedUrl);
            verify(googleOAuthProperties).buildAuthorizationUrl(state);
        }

        @Test
        @DisplayName("state가 null이면 GoogleOAuthProperties에서 예외를 발생시킨다")
        void getAuthorizationUrl_nullState_throwsException() {
            // given
            when(googleOAuthProperties.buildAuthorizationUrl(null))
                    .thenThrow(new IllegalArgumentException("state 매개변수는 필수입니다"));

            // when & then
            assertThatThrownBy(() -> googleLoginService.getAuthorizationUrl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("state 매개변수는 필수입니다");
        }

        @Test
        @DisplayName("state가 빈 문자열이면 GoogleOAuthProperties에서 예외를 발생시킨다")
        void getAuthorizationUrl_emptyState_throwsException() {
            // given
            when(googleOAuthProperties.buildAuthorizationUrl(""))
                    .thenThrow(new IllegalArgumentException("state 매개변수는 필수입니다"));

            // when & then
            assertThatThrownBy(() -> googleLoginService.getAuthorizationUrl(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("state 매개변수는 필수입니다");
        }
    }

    @Nested
    @DisplayName("getAccessToken")
    class GetAccessTokenTest {

        @Test
        @DisplayName("정상적으로 액세스 토큰을 획득한다")
        void getAccessToken_success() throws JsonProcessingException {
            // given
            String authCode = "test-auth-code";
            String tokenUri = "https://oauth2.googleapis.com/token";
            String expectedAccessToken = "test-access-token";
            
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            
            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode accessTokenNode = mock(JsonNode.class);
            String mockResponse = "{\"access_token\":\"test-access-token\"}";

            when(googleOAuthProperties.getTokenUri()).thenReturn(tokenUri);
            when(googleOAuthProperties.buildTokenRequestBody(authCode)).thenReturn("grant_type=authorization_code&client_id=test&client_secret=test&redirect_uri=test&code=test-auth-code");
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(tokenUri)).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
            when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(mockResponse);
            when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);
            when(mockJsonNode.get("access_token")).thenReturn(accessTokenNode);
            when(accessTokenNode.asText()).thenReturn(expectedAccessToken);

            // when
            String result = googleLoginService.getAccessToken(authCode);

            // then
            assertThat(result).isEqualTo(expectedAccessToken);
            verify(restClient).post();
            verify(requestBodyUriSpec).uri(tokenUri);
        }

        @Test
        @DisplayName("인증 코드가 null이면 예외를 발생시킨다")
        void getAccessToken_nullAuthCode_throwsException() {
            // when & then
            assertThatThrownBy(() -> googleLoginService.getAccessToken(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 코드는 필수입니다.");
        }

        @Test
        @DisplayName("인증 코드가 빈 문자열이면 예외를 발생시킨다")
        void getAccessToken_emptyAuthCode_throwsException() {
            // when & then
            assertThatThrownBy(() -> googleLoginService.getAccessToken(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 코드는 필수입니다.");
        }

        @Test
        @DisplayName("외부 API 호출 실패 시 BusinessException을 발생시킨다")
        void getAccessToken_apiFailure_throwsBusinessException() {
            // given
            String authCode = "test-auth-code";
            String tokenUri = "https://oauth2.googleapis.com/token";
            
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            
            when(googleOAuthProperties.getTokenUri()).thenReturn(tokenUri);
            when(googleOAuthProperties.buildTokenRequestBody(authCode)).thenReturn("grant_type=authorization_code&client_id=test&client_secret=test&redirect_uri=test&code=test-auth-code");
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(tokenUri)).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
            when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenThrow(new RuntimeException("API Error"));

            // when & then
            assertThatThrownBy(() -> googleLoginService.getAccessToken(authCode))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        @Test
        @DisplayName("JSON 파싱 실패 시 BusinessException을 발생시킨다")
        void getAccessToken_jsonParsingFailure_throwsBusinessException() throws JsonProcessingException {
            // given
            String authCode = "test-auth-code";
            String tokenUri = "https://oauth2.googleapis.com/token";
            String mockResponse = "invalid-json";
            
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            
            when(googleOAuthProperties.getTokenUri()).thenReturn(tokenUri);
            when(googleOAuthProperties.buildTokenRequestBody(authCode)).thenReturn("grant_type=authorization_code&client_id=test&client_secret=test&redirect_uri=test&code=test-auth-code");
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(tokenUri)).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
            when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(mockResponse);
            when(objectMapper.readTree(mockResponse)).thenThrow(new RuntimeException("JSON parsing failed"));

            // when & then
            assertThatThrownBy(() -> googleLoginService.getAccessToken(authCode))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        @Test
        @DisplayName("응답에 access_token이 없으면 BusinessException을 발생시킨다")
        void getAccessToken_noAccessToken_throwsBusinessException() throws JsonProcessingException {
            // given
            String authCode = "test-auth-code";
            String tokenUri = "https://oauth2.googleapis.com/token";
            
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            
            JsonNode mockJsonNode = mock(JsonNode.class);
            String mockResponse = "{\"error\":\"invalid_grant\"}";

            when(googleOAuthProperties.getTokenUri()).thenReturn(tokenUri);
            when(googleOAuthProperties.buildTokenRequestBody(authCode)).thenReturn("grant_type=authorization_code&client_id=test&client_secret=test&redirect_uri=test&code=test-auth-code");
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(tokenUri)).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
            when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(mockResponse);
            when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);
            when(mockJsonNode.get("access_token")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> googleLoginService.getAccessToken(authCode))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
    }

    @Nested
    @DisplayName("getMemberInfo")
    class GetMemberInfoTest {

        @Test
        @DisplayName("정상적으로 사용자 정보를 획득한다")
        void getMemberInfo_success() throws JsonProcessingException {
            // given
            String accessToken = "test-access-token";
            String memberInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";
            
            RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            
            JsonNode mockJsonNode = mock(JsonNode.class);
            String mockResponse = "{\"id\":\"123\",\"email\":\"test@example.com\",\"name\":\"Test User\",\"picture\":\"https://example.com/picture.jpg\"}";

            when(googleOAuthProperties.getMemberInfoUri()).thenReturn(memberInfoUri);
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(memberInfoUri)).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(mockResponse);
            when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);
            when(mockJsonNode.get("id")).thenReturn(mock(JsonNode.class));
            when(mockJsonNode.get("email")).thenReturn(mock(JsonNode.class));
            when(mockJsonNode.get("name")).thenReturn(mock(JsonNode.class));
            when(mockJsonNode.get("picture")).thenReturn(mock(JsonNode.class));
            when(mockJsonNode.get("id").asText()).thenReturn("123");
            when(mockJsonNode.get("email").asText()).thenReturn("test@example.com");
            when(mockJsonNode.get("name").asText()).thenReturn("Test User");
            when(mockJsonNode.get("picture").asText()).thenReturn("https://example.com/picture.jpg");

            // when
            SocialMemberInfo result = googleLoginService.getMemberInfo(accessToken);

            // then
            assertThat(result).isNotNull();
            assertThat(result.socialId()).isEqualTo("123");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.nickname()).isEqualTo("Test User");
            assertThat(result.profileImage()).isEqualTo("https://example.com/picture.jpg");
            assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
        }

        @Test
        @DisplayName("액세스 토큰이 null이면 예외를 발생시킨다")
        void getMemberInfo_nullAccessToken_throwsException() {
            // when & then
            assertThatThrownBy(() -> googleLoginService.getMemberInfo(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("액세스 토큰은 필수입니다.");
        }

        @Test
        @DisplayName("액세스 토큰이 빈 문자열이면 예외를 발생시킨다")
        void getMemberInfo_emptyAccessToken_throwsException() {
            // when & then
            assertThatThrownBy(() -> googleLoginService.getMemberInfo(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("액세스 토큰은 필수입니다.");
        }
    }

    @Nested
    @DisplayName("processLogin")
    class ProcessLoginTest {

        @Test
        @DisplayName("정상적으로 소셜 로그인을 처리한다")
        void processLogin_success() throws JsonProcessingException {
            // given
            String authCode = "test-auth-code";
            String accessToken = "test-access-token";
            String memberInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";
            String tokenUri = "https://oauth2.googleapis.com/token";
            
            RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            
            RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec memberInfoResponseSpec = mock(RestClient.ResponseSpec.class);
            
            JsonNode tokenJsonNode = mock(JsonNode.class);
            JsonNode accessTokenNode = mock(JsonNode.class);
            JsonNode memberInfoJsonNode = mock(JsonNode.class);
            String tokenResponse = "{\"access_token\":\"test-access-token\"}";
            String memberInfoResponse = "{\"id\":\"123\",\"email\":\"test@example.com\",\"name\":\"Test User\",\"picture\":\"https://example.com/picture.jpg\"}";

            Member mockMember = mock(Member.class);
            SocialMemberInfo mockSocialMemberInfo = new SocialMemberInfo("123", "test@example.com", "Test User", "https://example.com/picture.jpg", SocialProvider.GOOGLE);
            SocialLoginResponse mockLoginResponse = mock(SocialLoginResponse.class);

            when(googleOAuthProperties.getTokenUri()).thenReturn(tokenUri);
            when(googleOAuthProperties.getMemberInfoUri()).thenReturn(memberInfoUri);
            when(googleOAuthProperties.buildTokenRequestBody(authCode)).thenReturn("grant_type=authorization_code&client_id=test&client_secret=test&redirect_uri=test&code=test-auth-code");
            
            // Token request mocking
            when(restClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(tokenUri)).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
            when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenReturn(tokenResponse);
            when(objectMapper.readTree(tokenResponse)).thenReturn(tokenJsonNode);
            when(tokenJsonNode.get("access_token")).thenReturn(accessTokenNode);
            when(accessTokenNode.asText()).thenReturn(accessToken);
            
            // Member info request mocking
            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(memberInfoUri)).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(memberInfoResponseSpec);
            when(memberInfoResponseSpec.onStatus(any(), any())).thenReturn(memberInfoResponseSpec);
            when(memberInfoResponseSpec.body(String.class)).thenReturn(memberInfoResponse);
            when(objectMapper.readTree(memberInfoResponse)).thenReturn(memberInfoJsonNode);
            when(memberInfoJsonNode.get("id")).thenReturn(mock(JsonNode.class));
            when(memberInfoJsonNode.get("email")).thenReturn(mock(JsonNode.class));
            when(memberInfoJsonNode.get("name")).thenReturn(mock(JsonNode.class));
            when(memberInfoJsonNode.get("picture")).thenReturn(mock(JsonNode.class));
            when(memberInfoJsonNode.get("id").asText()).thenReturn("123");
            when(memberInfoJsonNode.get("email").asText()).thenReturn("test@example.com");
            when(memberInfoJsonNode.get("name").asText()).thenReturn("Test User");
            when(memberInfoJsonNode.get("picture").asText()).thenReturn("https://example.com/picture.jpg");
            
            when(memberService.findOrCreateMember(any(SocialMemberInfo.class))).thenReturn(mockMember);
            when(mockMember.getId()).thenReturn(1L);
            when(mockMember.getEmail()).thenReturn("test@example.com");
            when(mockMember.getNickname()).thenReturn("Test User");
            when(mockMember.getProfileImage()).thenReturn("https://example.com/picture.jpg");
            when(mockMember.getRole()).thenReturn(Role.NORMAL);
            
            when(accessTokenGenerator.generateAccessToken(anyLong(), anyString(), any(), any())).thenReturn("jwt-access-token");
            when(refreshTokenGenerator.generateRefreshToken(anyLong(), any())).thenReturn("jwt-refresh-token");
            when(accessTokenGenerator.getExpirySeconds()).thenReturn(3600L);

            // when
            SocialLoginResponse result = googleLoginService.processLogin(authCode);

            // then
            assertThat(result).isNotNull();
            verify(memberService).findOrCreateMember(any(SocialMemberInfo.class));
        }

        @Test
        @DisplayName("인증 코드가 null이면 예외를 발생시킨다")
        void processLogin_nullAuthCode_throwsException() {
            // when & then
            assertThatThrownBy(() -> googleLoginService.processLogin(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 코드는 필수입니다.");
        }

        @Test
        @DisplayName("인증 코드가 빈 문자열이면 예외를 발생시킨다")
        void processLogin_emptyAuthCode_throwsException() {
            // when & then
            assertThatThrownBy(() -> googleLoginService.processLogin(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 코드는 필수입니다.");
        }
    }

    @Nested
    @DisplayName("getProvider")
    class GetProviderTest {

        @Test
        @DisplayName("GOOGLE 제공자를 반환한다")
        void getProvider_returnsGoogle() {
            // when
            SocialProvider result = googleLoginService.getProvider();

            // then
            assertThat(result).isEqualTo(SocialProvider.GOOGLE);
        }
    }
} 