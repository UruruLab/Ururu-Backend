package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.member.domain.dto.request.ShippingAddressRequest;
import com.ururulab.ururu.member.domain.dto.response.ShippingAddressListResponse;
import com.ururulab.ururu.member.domain.dto.response.ShippingAddressResponse;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;
import com.ururulab.ururu.member.service.ShippingAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/{memberId}/shipping-addresses")
@RequiredArgsConstructor
@Tag(name = "배송지 관리", description = "회원 배송지 관리 API")
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;

    @Operation(summary = "배송지 목록 조회", description = "특정 회원의 모든 배송지를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송지 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponseFormat<ShippingAddressListResponse>> getShippingAddress(
            @PathVariable final Long memberId
    ) {
        final List<ShippingAddressResponse> addresses = shippingAddressService.getShippingAddresses(memberId)
                .stream()
                .map(ShippingAddressResponse::from)
                .toList();

        final ShippingAddressListResponse response = ShippingAddressListResponse.from(addresses);

        return ResponseEntity.ok(
                ApiResponseFormat.success("배송지 목록을 조회했습니다.", response)
        );
    }

    @Operation(summary = "배송지 생성", description = "새로운 배송지를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "배송지 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponseFormat<ShippingAddressResponse>> createShippingAddress(
            @PathVariable Long memberId,
            @Valid @RequestBody final ShippingAddressRequest request
    ) {
        final ShippingAddress shippingAddress = shippingAddressService.createShippingAddress(memberId, request);
        final ShippingAddressResponse response = ShippingAddressResponse.from(shippingAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("배송지가 등록되었습니다.", response));
    }

    @Operation(summary = "배송지 수정", description = "기존 배송지 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송지 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음")
    })
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponseFormat<ShippingAddressResponse>> updateShippingAddress(
            @PathVariable final Long memberId,
            @PathVariable final Long addressId,
            @Valid @RequestBody final ShippingAddressRequest request
    ) {
        final ShippingAddress shippingAddress = shippingAddressService.updateShippingAddress(memberId, addressId, request);
        final ShippingAddressResponse response = ShippingAddressResponse.from(shippingAddress);

        return ResponseEntity.ok(
                ApiResponseFormat.success("배송지가 수정되었습니다.", response)
        );
    }

    @Operation(summary = "배송지 삭제", description = "배송지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송지 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음")
    })
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponseFormat<Void>> deleteShippingAddress(
            @PathVariable final Long memberId,
            @PathVariable final Long addressId
    ) {
        shippingAddressService.deleteShippingAddress(memberId, addressId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("배송지가 삭제되었습니다.")
        );
    }
}
