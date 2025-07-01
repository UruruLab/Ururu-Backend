package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;

/**
 * 소셜 로그인 서비스 공통 인터페이스.
 *
 * <p>각 소셜 제공자(Kakao, Google)에 대한 공통 동작을 정의하며,
 * 인증 URL 생성, 토큰 교환, 사용자 정보 조회, 로그인 처리 등의 기능을 추상화합니다.</p>
 */
public interface SocialLoginService {

    /**
     * 소셜 제공자의 인증 URL을 생성합니다.
     *
     * @param state CSRF 방지용 상태값 (null이 될 수 있음, 제공자에 따라 선택적)
     * @return 인증 요청 URL
     * @throws IllegalArgumentException 필수 파라미터가 누락된 경우
     */
    String getAuthorizationUrl(String state);

    /**
     * 인증 코드를 통해 액세스 토큰을 획득합니다.
     *
     * @param code 소셜 제공자로부터 받은 인증 코드
     * @return 액세스 토큰
     * @throws IllegalArgumentException 인증 코드가 유효하지 않은 경우
     * @throws com.ururulab.ururu.global.exception.BusinessException 토큰 교환 실패 시
     */
    String getAccessToken(String code);

    /**
     * 액세스 토큰을 통해 사용자 정보를 조회합니다.
     *
     * @param accessToken 소셜 제공자의 액세스 토큰
     * @return 표준화된 사용자 정보
     * @throws IllegalArgumentException 액세스 토큰이 유효하지 않은 경우
     * @throws com.ururulab.ururu.global.exception.BusinessException 사용자 정보 조회 실패 시
     */
    SocialMemberInfo getMemberInfo(String accessToken);

    /**
     * 소셜 로그인 전체 과정을 처리합니다.
     *
     * @param code 인증 코드
     * @return JWT 토큰이 포함된 로그인 응답
     * @throws com.ururulab.ururu.global.exception.BusinessException 로그인 처리 실패 시
     */
    SocialLoginResponse processLogin(String code);
    /**
     * 이 서비스가 지원하는 소셜 제공자 정보를 반환합니다.
     *
     * @return SocialProvider enum 값
     */
    SocialProvider getProvider();
}