package com.ururulab.ururu.global.exception;

import com.ururulab.ururu.global.auth.exception.SocialLoginException;
import com.ururulab.ururu.global.auth.exception.SocialTokenExchangeException;
import com.ururulab.ururu.global.auth.exception.SocialMemberInfoException;
import com.ururulab.ururu.global.auth.exception.UnsupportedSocialProviderException;
import com.ururulab.ururu.global.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                .body(ApiResponse.error(exception.getMessage()));
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
                .body(ApiResponse.error("소셜 로그인 인증에 실패했습니다."));
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
                .body(ApiResponse.error("회원 정보를 가져올 수 없습니다."));
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
                .body(ApiResponse.error("소셜 로그인 처리 중 오류가 발생했습니다."));
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
                .map(error -> error.getDefaultMessage())
                .orElse("요청 데이터가 유효하지 않습니다.");

        log.warn("Validation failed: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
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
                .body(ApiResponse.error(exception.getMessage()));
    }

    /**
     * 예상하지 못한 모든 예외 처리.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(final Exception exception) {
        log.error("Unexpected error occurred: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }
}