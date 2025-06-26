package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.request.RefreshTokenRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.exception.InvalidRefreshTokenException;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.global.common.dto.ApiResponseFormat;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
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
        String newAccessToken;
        try {
            newAccessToken = jwtRefreshService.refreshAccessToken(request.refreshToken());
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponseFormat.fail("토큰 갱신 실패: " + e.getMessage()));
        }
        String refreshToken = request.refreshToken();
        Long expiresIn = jwtTokenProvider.getAccessTokenExpiry();
        Long memberId = jwtTokenProvider.getMemberId(newAccessToken);
        String email = jwtTokenProvider.getEmail(newAccessToken);
        String role = jwtTokenProvider.getRole(newAccessToken);

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
            return ResponseEntity.badRequest()
                    .body(ApiResponseFormat.fail("Authorization 헤더가 필요합니다."));
        }
        String accessToken = authorization.substring(7);
        try {
            Long memberId = jwtTokenProvider.getMemberId(accessToken);
            jwtRefreshService.logout(memberId, accessToken);
            return ResponseEntity.ok(ApiResponseFormat.success("로그아웃되었습니다."));
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponseFormat.fail("유효하지 않은 토큰입니다."));
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponseFormat.fail("일시적인 서버 오류입니다."));
        }
    }
}
