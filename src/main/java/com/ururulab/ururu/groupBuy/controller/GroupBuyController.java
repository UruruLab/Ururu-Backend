package com.ururulab.ururu.groupBuy.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.groupBuy.controller.dto.request.GroupBuyRequest;
import com.ururulab.ururu.groupBuy.controller.dto.response.GroupBuyCreateResponse;
import com.ururulab.ururu.groupBuy.service.GroupBuyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "공동구매", description = "공동구매 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/groupbuy/{sellerId}")  // sellerId를 경로에 포함, JWT 구현 완료 후 수정
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    @Operation(summary = "공동구매 등록", description = "판매자가 새로운 공동구매를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "공동구매가 성공적으로 등록되었습니다."),
            @ApiResponse(responseCode = "400", description = """
                잘못된 요청 데이터입니다:
                - 할인 단계 정보 파싱 실패
                - 잘못된 시작일/종료일 설정
                - 할인율 또는 수량 설정 오류
                - 할인 단계 중복 또는 초과
                - 재고보다 큰 최소 수량 설정
                """),
            @ApiResponse(responseCode = "403", description = "상품의 판매자와 공동구매 등록자가 일치하지 않습니다."),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 판매자 또는 상품입니다."),
            @ApiResponse(responseCode = "409", description = "중복된 공동구매가 존재합니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseFormat<GroupBuyCreateResponse>> createGroupBuy(
            @PathVariable Long sellerId,
            // @AuthenticationPrincipal CustomUserDetails userDetails, // JWT 인증 구현 후 주석 제거
            @Valid @RequestPart("request") GroupBuyRequest groupBuyRequest,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages
    ) {
        // JWT 인증 구현 후 sellerId 추출 로직
        // Long sellerId = userDetails.getSellerId();

        GroupBuyCreateResponse response = groupBuyService.createGroupBuy(groupBuyRequest, sellerId, thumbnail, detailImages);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("공동구매가 성공적으로 등록되었습니다.", response));
    }

}
