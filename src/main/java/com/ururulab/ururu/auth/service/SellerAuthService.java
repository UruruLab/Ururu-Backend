package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.service.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 판매자 인증 서비스.
 * 
 * 이메일+비밀번호 로그인을 처리하며, 소셜 로그인과 동일한 JWT 토큰 구조를 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class SellerAuthService {

    private final SellerService sellerService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final JwtRefreshService jwtRefreshService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 판매자 로그인 처리.
     *
     * @param request 로그인 요청 정보
     * @return JWT 토큰이 포함된 로그인 응답
     * @throws BusinessException 인증 실패 시
     */
    public SocialLoginResponse login(final SellerLoginRequest request) {
        // 판매자 조회 (이메일로 조회하는 메서드가 없으므로 Repository에서 직접 조회)
        final Seller seller = sellerService.findByEmail(request.email());
        
        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), seller.getPassword())) {
            log.warn("Invalid password attempt for seller email: {}", request.email());
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }
        
        // 계정 상태 확인 (삭제된 계정인지 확인)
        if (seller.getIsDeleted()) {
            log.warn("Deleted seller login attempt: {}", request.email());
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }
        
        // JWT 토큰 생성 (판매자는 SELLER 역할)
        final String accessToken = jwtTokenProvider.generateAccessToken(
                seller.getId(),
                seller.getEmail(),
                "SELLER", // 판매자 역할
                "SELLER"
        );
        
        final String refreshToken = jwtTokenProvider.generateRefreshToken(seller.getId(), "SELLER");
        
        // Refresh token을 Redis에 저장
        jwtRefreshService.storeRefreshToken(seller.getId(), "SELLER", refreshToken);
        
        // 응답 생성
        final SocialLoginResponse.MemberInfo memberInfo = SocialLoginResponse.MemberInfo.of(
                seller.getId(),
                seller.getEmail(),
                seller.getName(), // 브랜드명
                seller.getImage(), // 브랜드 대표 이미지
                "SELLER"
        );
        
        log.info("Seller login successful: {} (ID: {})", seller.getEmail(), seller.getId());
        
        return SocialLoginResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiry(),
                memberInfo
        );
    }

    /**
     * 판매자 로그아웃 처리.
     *
     * @param authorization Authorization 헤더 값
     */
    public void logout(final String authorization) {
        jwtRefreshService.logout(authorization);
        log.info("Seller logout completed");
    }
} 