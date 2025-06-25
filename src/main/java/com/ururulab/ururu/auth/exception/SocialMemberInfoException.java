package com.ururulab.ururu.auth.exception;

/**
 * 소셜 회원 정보 조회 실패 예외.
 *
 * <p>액세스 토큰을 통해 회원 정보를 조회하는 과정에서 실패할 때 발생합니다.</p>
 */
public final class SocialMemberInfoException extends SocialLoginException {

    public SocialMemberInfoException(final String message) {
        super(message);
    }

    public SocialMemberInfoException(final String message, final Throwable cause) {
        super(message, cause);
    }
}