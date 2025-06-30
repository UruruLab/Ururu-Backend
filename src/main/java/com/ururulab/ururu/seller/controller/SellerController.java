package com.ururulab.ururu.seller.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.common.util.MaskingUtils;
import com.ururulab.ururu.seller.domain.dto.request.SellerSignupRequest;
import com.ururulab.ururu.seller.domain.dto.response.SellerAvailabilityResponse;
import com.ururulab.ururu.seller.domain.dto.response.SellerResponse;
import com.ururulab.ururu.seller.domain.dto.response.SellerSignupResponse;
import com.ururulab.ururu.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /**
     * 판매자 회원가입
     * @param request 판매자 회원가입 요청
     * @return 회원가입 결과
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponseFormat<SellerSignupResponse>> signup(
            @Valid @RequestBody final SellerSignupRequest request
    ) {
        final SellerSignupResponse response = sellerService.signup(request);
        log.info("판매자 회원가입 API 호출 성공: ID={}", response.id());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("판매자 회원가입이 완료되었습니다.", response));
    }

    /**
     * 이메일 중복 체크
     * @param email 체크할 이메일
     * @return 가용성 결과
     */
    @GetMapping("/check/email")
    public ResponseEntity<ApiResponseFormat<SellerAvailabilityResponse>> checkEmailAvailability(
            @RequestParam final String email
    ) {
        final SellerAvailabilityResponse response = sellerService.checkEmailAvailability(email);
        log.debug("이메일 중복 체크 API 호출: {}", MaskingUtils.maskEmail(email));
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("이메일 가용성 체크가 완료되었습니다.", response));
    }

    /**
     * 사업자등록번호 중복 체크
     * @param businessNumber 체크할 사업자등록번호
     * @return 가용성 결과
     */
    @GetMapping("/check/business-number")
    public ResponseEntity<ApiResponseFormat<SellerAvailabilityResponse>> checkBusinessNumberAvailability(
            @RequestParam final String businessNumber
    ) {
        final SellerAvailabilityResponse response = sellerService.checkBusinessNumberAvailability(businessNumber);
        log.debug("사업자등록번호 중복 체크 API 호출: {}", MaskingUtils.maskBusinessNumber(businessNumber));
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("사업자등록번호 가용성 체크가 완료되었습니다.", response));
    }

    /**
     * 브랜드명 중복 체크
     * @param name 체크할 브랜드명
     * @return 가용성 결과
     */
    @GetMapping("/check/name")
    public ResponseEntity<ApiResponseFormat<SellerAvailabilityResponse>> checkNameAvailability(
            @RequestParam final String name
    ) {
        final SellerAvailabilityResponse response = sellerService.checkNameAvailability(name);
        log.debug("브랜드명 중복 체크 API 호출: {}", name);
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("브랜드명 가용성 체크가 완료되었습니다.", response));
    }

    /**
     * 판매자 프로필 조회
     * @param sellerId 판매자 ID
     * @return 판매자 정보
     */
    @GetMapping("/{sellerId}")
    public ResponseEntity<ApiResponseFormat<SellerResponse>> getSellerProfile(
            @PathVariable final Long sellerId
    ) {
        final SellerResponse response = sellerService.getSellerProfile(sellerId);
        log.debug("판매자 프로필 조회 API 호출: ID={}", sellerId);
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("판매자 정보 조회가 완료되었습니다.", response));
    }
} 