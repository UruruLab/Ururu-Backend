package com.ururulab.ururu.seller.service;

import com.ururulab.ururu.global.util.MaskingUtils;
import com.ururulab.ururu.seller.dto.request.SellerSignupRequest;
import com.ururulab.ururu.seller.dto.response.SellerAvailabilityResponse;
import com.ururulab.ururu.seller.dto.response.SellerResponse;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 판매자 회원가입
     * 동시성 환경에서 중복 가입을 100% 막기 위한 안전장치
     * 1. 트랜잭션 범위 내에서 중복 체크 수행 (1차 방어)
     * 2. DB UNIQUE 제약조건이 최종 방어선 역할 (2차 방어)
     * @param request 판매자 회원가입 요청 정보
     * @return 생성된 판매자 정보
     */
    @Transactional
    public SellerResponse signup(final SellerSignupRequest request) {
        log.info("판매자 회원가입 시작: {}", MaskingUtils.maskEmail(request.email()));

        // 이메일 정규화 (소문자 변환)
        final String normalizedEmail = request.email().toLowerCase().trim();

        // 트랜잭션 범위 내에서 중복 체크 (1차 방어)
        validateSignupRequest(request, normalizedEmail);

        // 비밀번호 암호화
        final String encodedPassword = passwordEncoder.encode(request.password());

        // 판매자 엔티티 생성
        final Seller seller = Seller.of(
                request.name(),
                request.businessName(),
                request.ownerName(),
                request.businessNumber(),
                normalizedEmail,
                encodedPassword,
                request.phone(),
                request.image(),
                request.address1(),
                request.address2(),
                request.mailOrderNumber()
        );

        try {
            // 저장 (DB UNIQUE 제약조건이 2차 방어선)
            final Seller savedSeller = sellerRepository.save(seller);

            log.info("판매자 회원가입 성공: ID={}, 이메일={}", 
                    savedSeller.getId(), MaskingUtils.maskEmail(savedSeller.getEmail()));

            return SellerResponse.forSignup(savedSeller);

        } catch (DataIntegrityViolationException e) {
            // DB 레벨에서 중복 감지 (동시성 환경에서 최종 방어선)
            log.warn("DB 중복 제약조건 위반: {}", e.getMessage());
            throw new com.ururulab.ururu.global.exception.BusinessException(
                com.ururulab.ururu.global.exception.error.ErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * 이메일 중복 체크
     *
     * @param email 체크할 이메일
     * @return 가용성 응답
     */
    public SellerAvailabilityResponse checkEmailAvailability(final String email) {
        // 이메일 정규화 (소문자 변환)
        final String normalizedEmail = email.toLowerCase().trim();
        final boolean isAvailable = sellerRepository.isEmailAvailable(normalizedEmail);
        log.debug("이메일 가용성 체크: {} = {}", MaskingUtils.maskEmail(normalizedEmail), isAvailable);
        return SellerAvailabilityResponse.ofEmailAvailability(isAvailable);
    }

    /**
     * 사업자등록번호 중복 체크
     * @param businessNumber 체크할 사업자등록번호
     * @return 가용성 응답
     */
    public SellerAvailabilityResponse checkBusinessNumberAvailability(final String businessNumber) {
        final boolean isAvailable = sellerRepository.isBusinessNumberAvailable(businessNumber);
        log.debug("사업자등록번호 가용성 체크: {} = {}", MaskingUtils.maskBusinessNumber(businessNumber), isAvailable);
        return SellerAvailabilityResponse.ofBusinessNumberAvailability(isAvailable);
    }

    /**
     * 브랜드명 중복 체크
     * @param name 체크할 브랜드명
     * @return 가용성 응답
     */
    public SellerAvailabilityResponse checkNameAvailability(final String name) {
        final boolean isAvailable = sellerRepository.isNameAvailable(name);
        log.debug("브랜드명 가용성 체크: {} = {}", name, isAvailable);
        return SellerAvailabilityResponse.ofNameAvailability(isAvailable);
    }

    /**
     * 판매자 정보 조회
     * @param sellerId 판매자 ID
     * @return 판매자 정보
     */
    public SellerResponse getSellerProfile(final Long sellerId) {
        final Seller seller = findActiveSellerById(sellerId);
        return SellerResponse.from(seller);
    }


     //회원가입 요청 유효성 검증
    private void validateSignupRequest(final SellerSignupRequest request, final String normalizedEmail) {
        // 이메일 중복 체크
        if (!sellerRepository.isEmailAvailable(normalizedEmail)) {
            log.warn("이메일 중복: {}", MaskingUtils.maskEmail(normalizedEmail));
            throw new com.ururulab.ururu.global.exception.BusinessException(
                com.ururulab.ururu.global.exception.error.ErrorCode.DUPLICATE_EMAIL);
        }

        // 사업자등록번호 중복 체크
        if (!sellerRepository.isBusinessNumberAvailable(request.businessNumber())) {
            log.warn("사업자등록번호 중복: {}", MaskingUtils.maskBusinessNumber(request.businessNumber()));
            throw new com.ururulab.ururu.global.exception.BusinessException(
                com.ururulab.ururu.global.exception.error.ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        // 브랜드명 중복 체크
        if (!sellerRepository.isNameAvailable(request.name())) {
            log.warn("브랜드명 중복: {}", request.name());
            throw new com.ururulab.ururu.global.exception.BusinessException(
                com.ururulab.ururu.global.exception.error.ErrorCode.DUPLICATE_BRAND_NAME);
        }
    }

    /**
     * 이메일로 판매자 조회
     *
     * @param email 판매자 이메일
     * @return 판매자 엔티티
     * @throws BusinessException 판매자가 존재하지 않는 경우
     */
    public Seller findByEmail(final String email) {
        final String normalizedEmail = email.toLowerCase().trim();
        return sellerRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new com.ururulab.ururu.global.exception.BusinessException(
                    com.ururulab.ururu.global.exception.error.ErrorCode.SELLER_NOT_FOUND));
    }

    // 활성 판매자 조회
    private Seller findActiveSellerById(final Long sellerId) {
        return sellerRepository.findActiveSeller(sellerId)
                .orElseThrow(() -> new com.ururulab.ururu.global.exception.BusinessException(
                    com.ururulab.ururu.global.exception.error.ErrorCode.SELLER_NOT_FOUND, sellerId));
    }
} 