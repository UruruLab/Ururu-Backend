package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.request.RefreshTokenRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.exception.RedisConnectionException;
import com.ururulab.ururu.auth.service.JwtRefreshService;
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

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> refreshToken(
            @Valid @RequestBody final RefreshTokenRequest request
    ) {
        final SocialLoginResponse response = jwtRefreshService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponseFormat.success("토큰이 갱신되었습니다.", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormat<Void>> logout(
            @RequestHeader("Authorization") final String authorization
    ) {
        try {
            jwtRefreshService.logout(authorization);
        } catch (final RedisConnectionFailureException e) {
            throw new RedisConnectionException("일시적인 서버 오류입니다.", e);
        }

        return ResponseEntity.ok(ApiResponseFormat.success("로그아웃되었습니다."));
    }
}