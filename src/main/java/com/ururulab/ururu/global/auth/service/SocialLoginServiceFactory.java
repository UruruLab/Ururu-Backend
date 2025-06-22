package com.ururulab.ururu.global.auth.service;

import com.ururulab.ururu.global.auth.exception.UnsupportedSocialProviderException;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 소셜 로그인 서비스 팩토리.
 *
 * <p>SocialProvider enum을 기반으로 적절한 SocialLoginService 구현체를 반환합니다.
 * 전략 패턴을 통해 소셜 제공자별 서비스를 관리합니다.</p>
 */
@Component
@RequiredArgsConstructor
public final class SocialLoginServiceFactory {

    private final Map<SocialProvider, SocialLoginService> socialLoginServices;

    /**
     * 소셜 제공자에 해당하는 로그인 서비스를 조회합니다.
     *
     * @param provider 소셜 제공자
     * @return 해당 제공자의 로그인 서비스
     * @throws IllegalArgumentException provider가 null인 경우
     * @throws UnsupportedSocialProviderException 지원하지 않는 제공자인 경우
     */
    public SocialLoginService getService(final SocialProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("소셜 제공자는 필수입니다.");
        }

        final SocialLoginService service = socialLoginServices.get(provider);
        if (service == null) {
            throw new UnsupportedSocialProviderException(
                    String.format("지원하지 않는 소셜 제공자입니다: %s", provider)
            );
        }

        return service;
    }
}