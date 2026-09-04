package com.ururulab.ururu.auth.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenExtractorTest {

    @Test
    void logoutExtractionAlwaysReturnsRawToken() {
        assertThat(TokenExtractor.extractTokenForLogout("Bearer header-token", "cookie-token"))
                .isEqualTo("header-token");
        assertThat(TokenExtractor.extractTokenForLogout(null, "cookie-token"))
                .isEqualTo("cookie-token");
        assertThat(TokenExtractor.extractTokenForLogout("bearer lower-case", null))
                .isEqualTo("lower-case");
    }
}
