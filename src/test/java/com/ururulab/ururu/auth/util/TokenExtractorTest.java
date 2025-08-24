package com.ururulab.ururu.auth.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * TokenExtractor 유틸리티 클래스 테스트.
 */
@DisplayName("TokenExtractor 테스트")
class TokenExtractorTest {

    @Test
    @DisplayName("유효한 JWT 토큰 형식 검증")
    void isValidAccessToken_ValidJwtFormat_ReturnsTrue() {
        // given
        String validToken = buildFakeJwt("sub", "1234567890");

        // when
        boolean result = TokenExtractor.isValidAccessToken(validToken);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("null 토큰 검증")
    void isValidAccessToken_NullToken_ReturnsFalse() {
        // when
        boolean result = TokenExtractor.isValidAccessToken(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증")
    void isValidAccessToken_EmptyToken_ReturnsFalse() {
        // when
        boolean result = TokenExtractor.isValidAccessToken("");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("공백 토큰 검증")
    void isValidAccessToken_BlankToken_ReturnsFalse() {
        // when
        boolean result = TokenExtractor.isValidAccessToken("   ");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 JWT 형식 검증")
    void isValidAccessToken_InvalidJwtFormat_ReturnsFalse() {
        // given
        String invalidToken = "invalid.token.format";

        // when
        boolean result = TokenExtractor.isValidAccessToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효한 Authorization 헤더 검증")
    void isValidAuthorizationHeader_ValidHeader_ReturnsTrue() {
        // given
        String validHeader = "Bearer " + buildFakeJwt("sub", "1234567890");

        // when
        boolean result = TokenExtractor.isValidAuthorizationHeader(validHeader);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("잘못된 Authorization 헤더 검증")
    void isValidAuthorizationHeader_InvalidHeader_ReturnsFalse() {
        // given
        String invalidHeader = "Basic dXNlcjpwYXNz";

        // when
        boolean result = TokenExtractor.isValidAuthorizationHeader(invalidHeader);

        // then
        assertThat(result).isFalse();
    }

    /**
     * 테스트용 가짜 JWT 토큰을 생성합니다.
     * 실제 비밀키나 페이로드를 포함하지 않는 안전한 테스트용 토큰입니다.
     *
     * @param claimKey 클레임 키
     * @param claimValue 클레임 값
     * @return 테스트용 JWT 토큰
     */
    private static String buildFakeJwt(String claimKey, String claimValue) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"" + claimKey + "\":\"" + claimValue + "\"}").getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("fake_signature_for_testing".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + signature;
    }
}
