package com.ururulab.ururu.auth.exception;

/**
 * JWT 토큰이 유효하지 않을 때 발생하는 예외.
 */
public final class InvalidJwtTokenException extends RuntimeException {
    public InvalidJwtTokenException(final String message) {
        super(message);
    }

    public InvalidJwtTokenException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
