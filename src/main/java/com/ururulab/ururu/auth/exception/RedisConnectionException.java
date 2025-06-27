package com.ururulab.ururu.auth.exception;

/**
 * Redis 연결 실패 시 발생하는 예외.
 */
public final class RedisConnectionException extends RuntimeException {
    public RedisConnectionException(final String message) {
        super(message);
    }

    public RedisConnectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
