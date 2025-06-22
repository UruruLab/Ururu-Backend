package com.ururulab.ururu.global.auth.service;

import com.ururulab.ururu.global.auth.exception.UnsupportedSocialProviderException;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 소셜 로그인 서비스 팩토리.
 *
 * <p>SocialProvider enum을 기반으로 적절한 SocialLoginService 구현체를 반환합니다.
 * 전략 패턴을 통해 소셜 제공자별 서비스를 관리합니다.</p>
 */
@Slf4j
@Component
public final class SocialLoginServiceFactory {

    private final Map<SocialProvider, SocialLoginService> socialLoginServices;

    /**
     * 생성자에서 SocialLoginService 구현체들을 Map 으로 변환하여 저장.
     *
     * @param socialLoginServiceList 스프링이 주입하는 SocialLoginService 구현체 리스트
     */
    public SocialLoginServiceFactory(final List<SocialLoginService> socialLoginServiceList) {
        this.socialLoginServices = socialLoginServiceList.stream()
                .collect(Collectors.toMap(
                        this::extractProviderFromService,
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("중복된 소셜 제공자 서비스가 감지되었습니다. Provider: {}, 기존: {}, 교체: {}",
                                    extractProviderFromService(existing), existing.getClass().getSimpleName(),
                                    replacement.getClass().getSimpleName());
                            return replacement; // 또는 existing을 유지
                            }
                ));

        log.info("Registered social login services: {}", socialLoginServices.keySet());
    }

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

    /**
     * 서비스 구현체로부터 SocialProvider 추출.
     *
     * <p>현재는 임시로 클래스명 기반으로 추출하며,
     * 실제 구현체에서는 getProvider() 메서드를 통해 반환하도록 수정 예정</p>
     */
    private SocialProvider extractProviderFromService(final SocialLoginService service) {
        return service.getProvider(); // SocialLoginService 인터페이스에 getProvider() 메서드 추가 필요
    }
}