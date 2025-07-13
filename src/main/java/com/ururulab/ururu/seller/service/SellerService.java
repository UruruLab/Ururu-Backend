package com.ururulab.ururu.seller.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
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
        // null 체크 강화
        if (request == null) {
            log.error("판매자 회원가입 요청이 null입니다.");
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "회원가입 요청은 null일 수 없습니다.");
        }

        log.info("판매자 회원가입 시작: {}", MaskingUtils.maskEmail(request.email()));

        // 이메일 정규화
        final String normalizedEmail = normalizeEmail(request.email());

        // 트랜잭션 범위 내에서 중복 체크 (1차 방어)
        validateSignupRequest(request, normalizedEmail);

        // 비밀번호 암호화
        final String encodedPassword = encodePassword(request.password());

        // 정규화된 데이터로 판매자 엔티티 생성
        final Seller seller = createSellerEntity(request, normalizedEmail, encodedPassword);

        try {
            // 저장 (DB UNIQUE 제약조건이 2차 방어선)
            final Seller savedSeller = sellerRepository.save(seller);

            log.info("판매자 회원가입 성공: ID={}, 이메일={}", 
                    savedSeller.getId(), MaskingUtils.maskEmail(savedSeller.getEmail()));

            return SellerResponse.forSignup(savedSeller);

        } catch (DataIntegrityViolationException e) {
            // DB 레벨에서 중복 감지 (동시성 환경에서 최종 방어선)
            log.warn("DB 중복 제약조건 위반: {}", e.getMessage());
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * 이메일 정규화
     */
    private String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "이메일은 필수입니다.");
        }
        return email.toLowerCase().trim();
    }

    /**
     * 비밀번호 암호화
     */
    private String encodePassword(final String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "비밀번호는 필수입니다.");
        }
        return passwordEncoder.encode(password);
    }

    /**
     * 판매자 엔티티 생성
     */
    private Seller createSellerEntity(final SellerSignupRequest request, final String normalizedEmail, final String encodedPassword) {
        // 통신판매업 번호 정규화
        final String normalizedMailOrderNumber = normalizeField(request.mailOrderNumber());

        // 사업자명 정규화
        final String normalizedBusinessName = normalizeField(request.businessName());

        // 대표자명 정규화
        final String normalizedOwnerName = normalizeField(request.ownerName());

        // 전화번호 정규화
        final String normalizedPhone = normalizeField(request.phone());

        // 우편번호 정규화
        final String normalizedZonecode = normalizeField(request.zonecode());

        // 주소 정규화 (앞뒤 공백만 제거, 중간 공백 유지)
        final String normalizedAddress1 = normalizeField(request.address1());
        final String normalizedAddress2 = normalizeField(request.address2());

        return Seller.of(
                request.name(),
                normalizedBusinessName,
                normalizedOwnerName,
                request.businessNumber(),
                normalizedEmail,
                encodedPassword,
                normalizedPhone,
                request.image(),
                normalizedZonecode,
                normalizedAddress1,
                normalizedAddress2,
                normalizedMailOrderNumber
        );
    }

    /**
     * 필드 정규화 (null 체크 포함)
     */
    private String normalizeField(final String field) {
        return field != null ? field.trim() : null;
    }

     //회원가입 요청 유효성 검증
    private void validateSignupRequest(final SellerSignupRequest request, final String normalizedEmail) {
        // 필수 필드 검증
        validateRequiredFields(request);
        
        // 이메일 중복 체크
        if (!sellerRepository.isEmailAvailable(normalizedEmail)) {
            log.warn("이메일 중복: {}", MaskingUtils.maskEmail(normalizedEmail));
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 사업자등록번호 정규화 후 중복 체크
        final String normalizedBusinessNumber = normalizeBusinessNumber(request.businessNumber());
        if (!sellerRepository.isBusinessNumberAvailable(normalizedBusinessNumber)) {
            log.warn("사업자등록번호 중복: {}", MaskingUtils.maskBusinessNumber(normalizedBusinessNumber));
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        // 브랜드명 정규화 후 중복 체크
        final String normalizedName = normalizeName(request.name());
        if (!sellerRepository.isNameAvailable(normalizedName)) {
            log.warn("브랜드명 중복: {}", normalizedName);
            throw new BusinessException(ErrorCode.DUPLICATE_BRAND_NAME);
        }
    }

    /**
     * 필수 필드 검증
     */
    private void validateRequiredFields(final SellerSignupRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "브랜드명은 필수입니다.");
        }
        if (request.businessNumber() == null || request.businessNumber().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "사업자등록번호는 필수입니다.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "비밀번호는 필수입니다.");
        }
    }

    /**
     * 사업자등록번호 정규화
     */
    private String normalizeBusinessNumber(final String businessNumber) {
        if (businessNumber == null || businessNumber.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "사업자등록번호는 필수입니다.");
        }
        return businessNumber.trim();
    }

    /**
     * 브랜드명 정규화
     */
    private String normalizeName(final String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "브랜드명은 필수입니다.");
        }
        return name.trim();
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
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
    }

    // 활성 판매자 조회
    private Seller findActiveSellerById(final Long sellerId) {
        return sellerRepository.findActiveSeller(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND, sellerId));
    }
} 