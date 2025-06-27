package com.ururulab.ururu.seller.service;

import com.ururulab.ururu.global.common.util.MaskingUtils;
import com.ururulab.ururu.seller.domain.dto.request.SellerSignupRequest;
import com.ururulab.ururu.seller.domain.dto.response.SellerAvailabilityResponse;
import com.ururulab.ururu.seller.domain.dto.response.SellerResponse;
import com.ururulab.ururu.seller.domain.dto.response.SellerSignupResponse;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import jakarta.persistence.EntityNotFoundException;
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
    public SellerSignupResponse signup(final SellerSignupRequest request) {
        log.info("판매자 회원가입 시작: {}", MaskingUtils.maskEmail(request.email()));

        // 트랜잭션 범위 내에서 중복 체크 (1차 방어)
        validateSignupRequest(request);

        // 비밀번호 암호화
        final String encodedPassword = passwordEncoder.encode(request.password());

        // 판매자 엔티티 생성
        final Seller seller = Seller.of(
                request.name(),
                request.businessName(),
                request.ownerName(),
                request.businessNumber(),
                request.email(),
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

            return SellerSignupResponse.of(savedSeller);

        } catch (DataIntegrityViolationException e) {
            // DB 레벨에서 중복 감지 (동시성 환경에서 최종 방어선)
            log.warn("DB 중복 제약조건 위반: {}", e.getMessage());
            throw new IllegalArgumentException("이미 사용 중인 정보입니다.");
        }
    }

    /**
     * 이메일 중복 체크
     * @param email 체크할 이메일
     * @return 가용성 응답
     */
    public SellerAvailabilityResponse checkEmailAvailability(final String email) {
        final boolean isAvailable = sellerRepository.isEmailAvailable(email);
        log.debug("이메일 가용성 체크: {} = {}", MaskingUtils.maskEmail(email), isAvailable);
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
        return SellerResponse.of(seller);
    }


     //회원가입 요청 유효성 검증
    private void validateSignupRequest(final SellerSignupRequest request) {
        // 이메일 중복 체크
        if (!sellerRepository.isEmailAvailable(request.email())) {
            log.warn("이메일 중복: {}", MaskingUtils.maskEmail(request.email()));
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 사업자등록번호 중복 체크
        if (!sellerRepository.isBusinessNumberAvailable(request.businessNumber())) {
            log.warn("사업자등록번호 중복: {}", MaskingUtils.maskBusinessNumber(request.businessNumber()));
            throw new IllegalArgumentException("이미 사용 중인 사업자등록번호입니다.");
        }

        // 브랜드명 중복 체크
        if (!sellerRepository.isNameAvailable(request.name())) {
            log.warn("브랜드명 중복: {}", request.name());
            throw new IllegalArgumentException("이미 사용 중인 브랜드명입니다.");
        }
    }

    // 활성 판매자 조회
    private Seller findActiveSellerById(final Long sellerId) {
        return sellerRepository.findActiveSeller(sellerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "판매자를 찾을 수 없습니다. ID: " + sellerId));
    }
} 