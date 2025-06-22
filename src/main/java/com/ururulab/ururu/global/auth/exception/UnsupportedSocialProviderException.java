package com.ururulab.ururu.global.auth.exception;

/**
 * 지원하지 않는 소셜 제공자 예외.
 */
public final class UnsupportedSocialProviderException extends SocialLoginException {

    public UnsupportedSocialProviderException(final String message) {
        super(message);
    }
}