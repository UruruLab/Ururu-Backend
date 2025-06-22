package com.ururulab.ururu.global.auth.exception;

/**
 * 소셜 토큰 교환 실패 예외.
 *
 * <p>인증 코드를 액세스 토큰으로 교환하는 과정에서 실패할 때 발생합니다.</p>
 */
public final class SocialTokenExchangeException extends SocialLoginException {

    public SocialTokenExchangeException(final String message) {
        super(message);
    }

    public SocialTokenExchangeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}