package com.ururulab.ururu.global.exception;

import com.ururulab.ururu.auth.exception.InvalidJwtTokenException;
import com.ururulab.ururu.auth.exception.InvalidRefreshTokenException;
import com.ururulab.ururu.auth.exception.MissingAuthorizationHeaderException;
import com.ururulab.ururu.auth.exception.RedisConnectionException;
import com.ururulab.ururu.auth.exception.SocialLoginException;
import com.ururulab.ururu.auth.exception.SocialTokenExchangeException;
import com.ururulab.ururu.auth.exception.SocialMemberInfoException;
import com.ururulab.ururu.auth.exception.UnsupportedSocialProviderException;
import com.ururulab.ururu.global.common.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 *
 * <p>애플리케이션 전반에서 발생하는 예외를 일관된 형태로 처리하고 클라이언트에게 적절한 응답을 제공합니다.</p>
 */
@Slf4j
@RestControllerAdvice
public final class GlobalExceptionHandler {

    /**
     * 지원하지 않는 소셜 제공자 예외 처리.
     */
    @ExceptionHandler(UnsupportedSocialProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedSocialProvider(
            final UnsupportedSocialProviderException exception
    ) {
        log.warn("Unsupported social provider: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(exception.getMessage()));
    }

    /**
     * 소셜 토큰 교환 실패 예외 처리.
     */
    @ExceptionHandler(SocialTokenExchangeException.class)
    public ResponseEntity<ApiResponse<Void>> handleSocialTokenExchange(
            final SocialTokenExchangeException exception
    ) {
        log.error("Social token exchange failed: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("소셜 로그인 인증에 실패했습니다."));
    }

    /**
     * 소셜 회원 정보 조회 실패 예외 처리.
     */
    @ExceptionHandler(SocialMemberInfoException.class)
    public ResponseEntity<ApiResponse<Void>> handleSocialMemberInfo(
            final SocialMemberInfoException exception
    ) {
        log.error("Social member info retrieval failed: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("회원 정보를 가져올 수 없습니다."));
    }

    /**
     * 일반적인 소셜 로그인 예외 처리.
     */
    @ExceptionHandler(SocialLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleSocialLogin(
            final SocialLoginException exception
    ) {
        log.error("Social login failed: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("소셜 로그인 처리 중 오류가 발생했습니다."));
    }

    /**
     * 요청 데이터 검증 실패 예외 처리.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            final MethodArgumentNotValidException exception
    ) {
        final String errorMessage = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("요청 데이터가 유효하지 않습니다.");

        log.warn("Validation failed: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(errorMessage));
    }

    /**
     * IllegalArgumentException 처리.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            final IllegalArgumentException exception
    ) {
        log.warn("Invalid argument: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(exception.getMessage()));
    }

    /**
     * JJWT 라이브러리 JWT 토큰 처리 예외 처리.
     */
    @ExceptionHandler({
            io.jsonwebtoken.JwtException.class,
            ExpiredJwtException.class,
            MalformedJwtException.class,
            SecurityException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleJjwtException(
            final RuntimeException exception
    ) {
        log.warn("JJWT processing failed: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("유효하지 않은 토큰입니다."));
    }

    /**
     * JWT 토큰 처리 중 발생하는 OAuth2 JWT 예외 처리.
     */
    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleOAuth2JwtException(
            final org.springframework.security.oauth2.jwt.JwtException exception
    ) {
        log.warn("OAuth2 JWT processing failed: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("유효하지 않은 토큰입니다."));
    }

    /**
     * JWT 토큰 유효성 검사 실패 예외 처리.
     */
    @ExceptionHandler(InvalidJwtTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJwtToken(
            final InvalidJwtTokenException exception
    ) {
        log.warn("Invalid JWT token: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("유효하지 않은 토큰입니다."));
    }

    /**
     * Refresh 토큰 유효성 검사 실패 예외 처리.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(
            final InvalidRefreshTokenException exception
    ) {
        log.warn("Invalid refresh token: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("토큰 갱신 실패: " + exception.getMessage()));
    }

    /**
     * Authorization 헤더 누락 예외 처리.
     */
    @ExceptionHandler(MissingAuthorizationHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingAuthorizationHeader(
            final MissingAuthorizationHeaderException exception
    ) {
        log.warn("Missing authorization header: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(exception.getMessage()));
    }

    /**
     * Redis 연결 실패 예외 처리.
     */
    @ExceptionHandler(RedisConnectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisConnection(
            final RedisConnectionException exception
    ) {
        log.error("Redis connection failed: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("일시적인 서버 오류입니다."));
    }

    /**
     * 예상하지 못한 모든 예외 처리.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(final Exception exception) {
        log.error("Unexpected error occurred: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("서버 내부 오류가 발생했습니다."));
    }
}