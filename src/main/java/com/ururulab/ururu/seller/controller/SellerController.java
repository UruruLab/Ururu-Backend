package com.ururulab.ururu.seller.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.util.MaskingUtils;
import com.ururulab.ururu.seller.dto.request.SellerSignupRequest;
import com.ururulab.ururu.seller.dto.response.SellerAvailabilityResponse;
import com.ururulab.ururu.seller.dto.response.SellerResponse;
import com.ururulab.ururu.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "판매자 관리", description = "판매자 회원가입 및 정보 관리 API")
public class SellerController {

    private final SellerService sellerService;

    /**
     * 판매자 회원가입
     * @param request 판매자 회원가입 요청
     * @return 회원가입 결과
     */
    @PostMapping("/signup")
    @Operation(
            summary = "판매자 회원가입",
            description = "새로운 판매자 계정을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (중복된 정보, 유효성 검증 실패)"
            )
    })
    public ResponseEntity<ApiResponseFormat<SellerResponse>> signup(
            @Parameter(description = "판매자 회원가입 정보", required = true)
            @Valid @RequestBody final SellerSignupRequest request
    ) {
        final SellerResponse response = sellerService.signup(request);
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
    @Operation(
            summary = "이메일 중복 체크",
            description = "판매자 회원가입 시 사용할 이메일의 중복 여부를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "중복 체크 완료",
                    content = @Content(schema = @Schema(implementation = SellerAvailabilityResponse.class))
            )
    })
    public ResponseEntity<ApiResponseFormat<SellerAvailabilityResponse>> checkEmailAvailability(
            @Parameter(description = "체크할 이메일 주소", required = true, example = "seller@example.com")
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
    @Operation(
            summary = "사업자등록번호 중복 체크",
            description = "판매자 회원가입 시 사용할 사업자등록번호의 중복 여부를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "중복 체크 완료",
                    content = @Content(schema = @Schema(implementation = SellerAvailabilityResponse.class))
            )
    })
    public ResponseEntity<ApiResponseFormat<SellerAvailabilityResponse>> checkBusinessNumberAvailability(
            @Parameter(description = "체크할 사업자등록번호", required = true, example = "1234567890")
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
    @Operation(
            summary = "브랜드명 중복 체크",
            description = "판매자 회원가입 시 사용할 브랜드명의 중복 여부를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "중복 체크 완료",
                    content = @Content(schema = @Schema(implementation = SellerAvailabilityResponse.class))
            )
    })
    public ResponseEntity<ApiResponseFormat<SellerAvailabilityResponse>> checkNameAvailability(
            @Parameter(description = "체크할 브랜드명", required = true, example = "우르르 뷰티")
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
    @Operation(
            summary = "판매자 프로필 조회",
            description = "특정 판매자의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "판매자를 찾을 수 없음"
            )
    })
    public ResponseEntity<ApiResponseFormat<SellerResponse>> getSellerProfile(
            @Parameter(description = "판매자 ID", required = true, example = "1")
            @PathVariable final Long sellerId
    ) {
        final SellerResponse response = sellerService.getSellerProfile(sellerId);
        log.debug("판매자 프로필 조회 API 호출: ID={}", sellerId);
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("판매자 정보 조회가 완료되었습니다.", response));
    }
} 