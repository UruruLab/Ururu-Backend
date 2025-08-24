package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
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
 * <p>이메일+비밀번호 로그인을 처리하며, 소셜 로그인과 동일한 JWT 토큰 구조를 사용합니다.
 * 판매자 계정의 상태 검증과 보안을 위한 다양한 검증을 수행합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class SellerAuthService {

    private final SellerService sellerService;
    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
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
        log.info("Seller login attempt: {}", request.email());
        
        final Seller seller = findAndValidateSeller(request.email());
        validatePassword(request.password(), seller.getPassword());
        validateAccountStatus(seller);
        
        final SocialLoginResponse loginResponse = createLoginResponse(seller);
        
        log.info("Seller login successful: {} (ID: {})", seller.getEmail(), seller.getId());
        
        return loginResponse;
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

    /**
     * 토큰으로 판매자 로그아웃 처리.
     *
     * @param accessToken 액세스 토큰
     */
    public void logoutWithToken(final String accessToken) {
        jwtRefreshService.logoutWithToken(accessToken);
        log.info("Seller logout with token completed");
    }

    // ==================== Private Helper Methods ====================

    /**
     * 판매자를 조회하고 기본 검증을 수행합니다.
     */
    private Seller findAndValidateSeller(final String email) {
        try {
            return sellerService.findByEmail(email);
        } catch (final BusinessException e) {
            if (e.getErrorCode() == ErrorCode.SELLER_NOT_FOUND) {
                log.warn("Seller not found for email: {}", email);
                throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
            }
            throw e;
        } catch (final Exception e) {
            log.warn("Seller not found for email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }
    }

    /**
     * 비밀번호를 검증합니다.
     */
    private void validatePassword(final String rawPassword, final String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("Invalid password attempt for seller email");
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }
    }

    /**
     * 계정 상태를 검증합니다.
     */
    private void validateAccountStatus(final Seller seller) {
        if (seller.getIsDeleted()) {
            log.warn("Deleted seller login attempt: {}", seller.getEmail());
            throw new BusinessException(ErrorCode.INACTIVE_ACCOUNT);
        }
    }

    /**
     * 로그인 응답을 생성합니다.
     */
    private SocialLoginResponse createLoginResponse(final Seller seller) {
        final String accessToken = accessTokenGenerator.generateAccessToken(
                seller.getId(),
                seller.getEmail(),
                UserRole.SELLER,
                UserType.SELLER
        );
        
        final String refreshToken = refreshTokenGenerator.generateRefreshToken(
                seller.getId(), 
                UserType.SELLER
        );
        
        // Refresh token을 Redis에 저장
        jwtRefreshService.storeRefreshToken(seller.getId(), UserType.SELLER.getValue(), refreshToken);
        
        // 응답 생성
        final SocialLoginResponse.MemberInfo memberInfo = SocialLoginResponse.MemberInfo.of(
                seller.getId(),
                seller.getEmail(),
                seller.getName(), // 브랜드명
                seller.getImage(), // 브랜드 대표 이미지
                UserType.SELLER.getValue()
        );
        
        return SocialLoginResponse.of(
                accessToken,
                refreshToken,
                accessTokenGenerator.getExpirySeconds(),
                memberInfo
        );
    }
} 