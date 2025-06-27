package com.ururulab.ururu.auth.exception;

/**
 * Authorization 헤더가 누락되었을 때 발생하는 예외.
 */
public final class MissingAuthorizationHeaderException extends RuntimeException {
    public MissingAuthorizationHeaderException(final String message) {
        super(message);
    }
}
