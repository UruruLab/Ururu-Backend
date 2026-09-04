package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2MemberServiceTest {

    @Mock private MemberService memberService;
    @Mock private AccessTokenGenerator accessTokenGenerator;
    @Mock private RefreshTokenGenerator refreshTokenGenerator;
    @Mock private JwtRefreshService jwtRefreshService;
    @Mock private Member member;

    private OAuth2MemberService service;

    @BeforeEach
    void setUp() {
        service = new OAuth2MemberService(
                memberService, accessTokenGenerator, refreshTokenGenerator, jwtRefreshService
        );
    }

    @Test
    void createsLocalJwtFromVerifiedGoogleIdentity() {
        when(memberService.findOrCreateMember(any())).thenReturn(member);
        when(member.getId()).thenReturn(1L);
        when(member.getEmail()).thenReturn("member@example.com");
        when(member.getNickname()).thenReturn("member");
        when(member.getRole()).thenReturn(Role.MEMBER);
        when(accessTokenGenerator.generateAccessToken(
                1L, "member@example.com", UserRole.MEMBER, UserType.MEMBER
        )).thenReturn("access");
        when(refreshTokenGenerator.generateRefreshToken(1L, UserType.MEMBER)).thenReturn("refresh");
        when(accessTokenGenerator.getExpirySeconds()).thenReturn(900L);

        final var result = service.login("google", Map.of(
                "sub", "google-id",
                "email", "member@example.com",
                "email_verified", true,
                "name", "member"
        ));

        assertThat(result.accessToken()).isEqualTo("access");
        verify(jwtRefreshService).storeRefreshToken(1L, "MEMBER", "refresh");
    }

    @Test
    void rejectsUnverifiedProviderEmail() {
        assertThatThrownBy(() -> service.login("google", Map.of(
                "sub", "google-id",
                "email", "member@example.com",
                "email_verified", false,
                "name", "member"
        ))).isInstanceOf(BusinessException.class);
    }
}
