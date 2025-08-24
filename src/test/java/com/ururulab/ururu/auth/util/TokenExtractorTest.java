package com.ururulab.ururu.auth.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenExtractor 유틸리티 클래스 테스트.
 */
@DisplayName("TokenExtractor 테스트")
class TokenExtractorTest {

    @Test
    @DisplayName("유효한 JWT 토큰 형식 검증")
    void isValidAccessToken_ValidJwtFormat_ReturnsTrue() {
        // given
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

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
        String validHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

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
}
