package com.ururulab.ururu.auth.exception;

/**
 * Refresh 토큰이 유효하지 않을 때 발생하는 예외.
 * (만료, 위조, 미인증 로그아웃 등의 경우)
 */
public final class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(final String message) {
        super(message);
    }
}
