package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenRoleCompatibilityTest {

    @Test
    void treatsLegacyNormalTokenAsMemberDuringRollout() {
        final JwtTokenProvider provider = mock(JwtTokenProvider.class);
        final TokenBlacklistStorage blacklist = mock(TokenBlacklistStorage.class);
        when(provider.isTokenExpired("legacy-token")).thenReturn(false);
        when(provider.isAccessToken("legacy-token")).thenReturn(true);
        when(provider.getTokenId("legacy-token")).thenReturn("jti");
        when(provider.getMemberId("legacy-token")).thenReturn(1L);
        when(provider.getUserType("legacy-token")).thenReturn("MEMBER");
        when(provider.getRole("legacy-token")).thenReturn("NORMAL");

        final TokenValidator.TokenValidationResult result =
                new TokenValidator(provider, blacklist).validateAccessToken("legacy-token");

        assertThat(result.role()).isEqualTo("MEMBER");
    }
}
