package com.ururulab.ururu.groupBuy.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyRequest;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyStatusUpdateRequest;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyCreatePageResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyCreateResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyDetailResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import com.ururulab.ururu.groupBuy.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "공동구매", description = "공동구매 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/groupbuy")  // sellerId를 경로에 포함, JWT 구현 완료 후 수정
@Slf4j
public class GroupBuyController {

    private final GroupBuyService groupBuyService;
    private final GroupBuyDetailService groupBuyDetailService;
    private final UpdateGroupBuyStatusService updateGroupBuyStatusService;
    private final GroupBuyListService groupBuyListService;
    private final GroupBuyProductService groupBuyProductService;
    private final GroupBuyDeleteService groupBuyDeleteService;

    //임시 - 추후 삭제
    private final GroupBuyRealtimeCloseService groupBuyRealtimeCloseService;

    @GetMapping("/{groupBuyId}/check-stock") //테스트용
    public ResponseEntity<String> checkStock(@PathVariable Long groupBuyId) {
        groupBuyRealtimeCloseService.checkAndCloseIfStockDepleted(groupBuyId);
        return ResponseEntity.ok("재고 체크 완료");
    }

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
    @PostMapping(value = "/{sellerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

    @Operation(
            summary = "구매자용 공동구매 상세 정보 조회",
            description = "특정 공동구매의 상세 정보를 조회합니다. 공동구매 정보, 옵션, 리워드, 총 주문수, 옵션의 현재 재고 등을 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공동구매 상세 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = "해당 공동구매를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{groupBuyId}")
    public ResponseEntity<ApiResponseFormat<GroupBuyDetailResponse>> getGroupBuyDetail(
            @PathVariable Long groupBuyId) {

        GroupBuyDetailResponse response = groupBuyDetailService.getPublicGroupBuyDetail(groupBuyId);
        return ResponseEntity.ok(ApiResponseFormat.success("공동구매 상세 정보를 성공적으로 조회했습니다.", response));
    }


    @Operation(
            summary = "판매자용 공동구매 상세 정보 조회",
            description = "특정 공동구매의 상세 정보를 조회합니다. 공동구매 정보, 옵션, 리워드, 총 주문수, 옵션의 현재 재고 등을 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공동구매 상세 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = """
                    - 해당 공동구매를 찾을 수 없습니다.
                    - 다른 판매자의 공동구매에 접근할 수 없습니다.
                    """),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 판매자입니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{sellerId}/{groupBuyId}")
    public ResponseEntity<ApiResponseFormat<GroupBuyDetailResponse>> getSellerGroupBuyDetail(
            @PathVariable Long sellerId,
            @PathVariable Long groupBuyId
    ) {

        GroupBuyDetailResponse response = groupBuyDetailService.getSellerGroupBuyDetail(sellerId, groupBuyId);
        return ResponseEntity.ok(ApiResponseFormat.success("공동구매 상세 정보를 성공적으로 조회했습니다.", response));
    }

    @Operation(
            summary = "공동구매 상태 업데이트 (DRAFT → OPEN)",
            description = "판매자가 DRAFT 상태의 공동구매를 OPEN 상태로 변경합니다. 시작일, 종료일, 재고 등의 조건을 검증합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "상태 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = """
                    - 공동구매 시작일이 아직 되지 않았습니다.
                    - 다른 판매자의 공동구매에 접근할 수 없습니다.
                    - 해당 공동구매를 찾을 수 없습니다.
                    - 현재 상태에서 요청한 상태로 변경할 수 없습니다.
                    """)
    })
    @PatchMapping("/{sellerId}/{groupBuyId}/status")
    public ResponseEntity<ApiResponseFormat<Void>> updateGroupBuyStatus(
            @PathVariable Long sellerId,
            @PathVariable Long groupBuyId,
            @Valid @RequestBody GroupBuyStatusUpdateRequest request) {

        // 상태 업데이트 (DRAFT → OPEN)
        updateGroupBuyStatusService.updateGroupBuyStatus(sellerId, groupBuyId, request);

        return ResponseEntity.ok(ApiResponseFormat.success(
                String.format("공동구매가 %s 상태로 변경되었습니다.", request.status().name()),
                null
        ));
    }

    @Operation(
            summary = "공동구매 목록 조회",
            description = "카테고리별, 정렬 기준별로 공동구매 목록을 조회합니다. 페이지네이션과 필터링을 지원합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공동구매 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "해당 카테고리를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponseFormat<List<GroupBuyListResponse>>> getGroupBuyList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "order_count") String sort) {
        List<GroupBuyListResponse> responses;

        responses = groupBuyListService.getGroupBuyList(categoryId, limit, sort);

        return ResponseEntity.ok(ApiResponseFormat.success("공동 구매 목록 조회에 성공하였습니다.", responses));
    }

    @Operation(summary = "공동구매 등록 페이지 데이터",
            description = "공동구매 등록 시 필요한 판매자의 상품과 옵션 정보를 조회합니다.")
    @GetMapping("/{sellerId}/create")
    public ResponseEntity<ApiResponseFormat<GroupBuyCreatePageResponse>> getGroupBuyCreateData(
            @Parameter(description = "판매자 ID", example = "1")
            @PathVariable Long sellerId) {

        GroupBuyCreatePageResponse response = groupBuyProductService.getGroupBuyCreateData(sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("공동구매 등록 페이지 데이터를 성공적으로 조회했습니다.", response));
    }

    @Operation(
            summary = "공동구매 삭제",
            description = "판매자가 본인의 공동구매 중 DRAFT 상태인 항목을 삭제합니다. 하드 딜리트 방식입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공동구매 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "DRAFT 상태인 공동구매만 삭제할 수 있습니다."),
            @ApiResponse(responseCode = "403", description = "다른 판매자의 공동구매에 접근할 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 공동구매를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{sellerId}/{groupBuyId}")
    public ResponseEntity<ApiResponseFormat<Void>> deleteGroupBuy(
            @PathVariable Long groupBuyId,
            @PathVariable Long sellerId
    ) {
        groupBuyDeleteService.deleteGroupBuy(groupBuyId, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("공동구매가 성공적으로 삭제되었습니다."));
    }
}
