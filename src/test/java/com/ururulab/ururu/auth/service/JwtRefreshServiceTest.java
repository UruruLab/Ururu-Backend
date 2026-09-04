package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.storage.RefreshTokenStorage;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtRefreshServiceTest {

    @Mock private AccessTokenGenerator accessTokenGenerator;
    @Mock private RefreshTokenGenerator refreshTokenGenerator;
    @Mock private RefreshTokenStorage refreshTokenStorage;
    @Mock private TokenBlacklistStorage tokenBlacklistStorage;
    @Mock private UserInfoService userInfoService;
    @Mock private TokenValidator tokenValidator;
    @InjectMocks private JwtRefreshService service;

    @Test
    void rejectsRefreshTokenThatWasAlreadyConsumed() {
        when(tokenValidator.validateRefreshToken("old-refresh"))
                .thenReturn(TokenValidator.TokenValidationResult.of(1L, "MEMBER", "old-jti", null));
        when(userInfoService.getUserInfo(1L, "MEMBER"))
                .thenReturn(UserInfoService.UserInfo.of("member@example.com", "MEMBER"));
        when(accessTokenGenerator.generateAccessToken(1L, "member@example.com", UserRole.MEMBER, UserType.MEMBER))
                .thenReturn("new-access");
        when(refreshTokenGenerator.generateRefreshToken(1L, UserType.MEMBER)).thenReturn("new-refresh");
        when(refreshTokenStorage.rotateRefreshToken("MEMBER", 1L, "old-refresh", "new-refresh"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.refreshAccessToken("old-refresh"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);

        verify(refreshTokenStorage).rotateRefreshToken("MEMBER", 1L, "old-refresh", "new-refresh");
    }

    @Test
    void revokesAccessAndRefreshTokensForTheWholeAccount() {
        service.revokeAllTokens(1L, UserType.MEMBER);

        verify(tokenBlacklistStorage).revokeUserTokens(1L, "MEMBER");
        verify(refreshTokenStorage).deleteAllRefreshTokens("MEMBER", 1L);
    }
}
