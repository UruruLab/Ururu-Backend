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
import com.ururulab.ururu.global.util.MaskingUtils;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.service.SellerService;
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
public final class SellerAuthService {

    private final SellerService sellerService;
    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final JwtRefreshService jwtRefreshService;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public SellerAuthService(
            final SellerService sellerService,
            final AccessTokenGenerator accessTokenGenerator,
            final RefreshTokenGenerator refreshTokenGenerator,
            final JwtRefreshService jwtRefreshService,
            final PasswordEncoder passwordEncoder) {
        this.sellerService = sellerService;
        this.accessTokenGenerator = accessTokenGenerator;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.jwtRefreshService = jwtRefreshService;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("invalid-login-password");
    }

    /**
     * 판매자 로그인 처리.
     *
     * @param request 로그인 요청 정보
     * @return JWT 토큰이 포함된 로그인 응답
     * @throws BusinessException 인증 실패 시
     */
    public SocialLoginResponse login(final SellerLoginRequest request) {
        // 판매자 조회 (이메일로 조회하는 메서드가 없으므로 Repository에서 직접 조회)
        final Seller seller;
        try {
            seller = sellerService.findByEmail(request.email());
        } catch (final BusinessException e) {
            if (e.getErrorCode() != ErrorCode.SELLER_NOT_FOUND) {
                throw e;
            }
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }
        
        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), seller.getPassword()) || seller.getIsDeleted()) {
            log.warn("Invalid seller login attempt: {}", MaskingUtils.maskEmail(request.email()));
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }
        
        // JWT 토큰 생성 (판매자는 SELLER 역할)
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
        
        log.info("Seller login successful: {} (ID: {})", MaskingUtils.maskEmail(seller.getEmail()), seller.getId());
        
        return SocialLoginResponse.of(
                accessToken,
                refreshToken,
                accessTokenGenerator.getExpirySeconds(),
                memberInfo
        );
    }

}
