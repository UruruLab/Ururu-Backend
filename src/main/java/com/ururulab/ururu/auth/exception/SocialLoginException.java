package com.ururulab.ururu.auth.exception;

/**
 * 소셜 로그인 관련 최상위 예외 클래스.
 */
public class SocialLoginException extends RuntimeException {

    public SocialLoginException(final String message) {
        super(message);
    }

    public SocialLoginException(final String message, final Throwable cause) {
        super(message, cause);
    }
}