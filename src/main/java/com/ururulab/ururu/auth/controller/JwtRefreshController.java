package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.request.RefreshTokenRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.exception.MissingAuthorizationHeaderException;
import com.ururulab.ururu.auth.exception.RedisConnectionException;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.global.common.dto.ApiResponseFormat;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public final class JwtRefreshController {
    private final JwtRefreshService jwtRefreshService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        final String newAccessToken = jwtRefreshService.refreshAccessToken(request.refreshToken());
        final String refreshToken = request.refreshToken();
        final Long expiresIn = jwtTokenProvider.getAccessTokenExpiry();
        final Long memberId = jwtTokenProvider.getMemberId(newAccessToken);
        final String email = jwtTokenProvider.getEmail(newAccessToken);
        final String role = jwtTokenProvider.getRole(newAccessToken);

        SocialLoginResponse responseBody = SocialLoginResponse.of(
                newAccessToken,
                refreshToken,
                expiresIn,
                SocialLoginResponse.MemberInfo.of(memberId, email, null, null)
        );
        return ResponseEntity.ok(ApiResponseFormat.success("토큰이 갱신되었습니다.", responseBody));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormat<Void>> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MissingAuthorizationHeaderException("Authorization 헤더가 필요합니다.");
        }

        final String accessToken = authorization.substring(7);
        final Long memberId = jwtTokenProvider.getMemberId(accessToken);

        try {
            jwtRefreshService.logout(memberId, accessToken);
        } catch (RedisConnectionFailureException e) {
            throw new RedisConnectionException("일시적인 서버 오류입니다.", e);
        }

        return ResponseEntity.ok(ApiResponseFormat.success("로그아웃되었습니다."));
    }
}
