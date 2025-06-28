package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.member.domain.dto.response.ShippingAddressListResponse;
import com.ururulab.ururu.member.domain.dto.response.ShippingAddressResponse;
import com.ururulab.ururu.member.service.ShippingAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
