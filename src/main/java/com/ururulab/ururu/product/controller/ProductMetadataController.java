package com.ururulab.ururu.product.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.product.dto.response.ProductMetadataResponse;
import com.ururulab.ururu.product.service.ProductMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products/create")
public class ProductMetadataController {

    private final ProductMetadataService productMetadataService;

    @Operation(
            summary = "상품 등록 메타데이터 조회",
            description = "카테고리 트리와 태그 리스트를 함께 반환합니다. 프론트에서 상품 등록/수정 시 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 메타데이터 조회 성공"),
            @ApiResponse(responseCode = "404", description = "카테고리 또는 태그 데이터가 없습니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponseFormat<ProductMetadataResponse>> getMetadata() {
        ProductMetadataResponse metadata = productMetadataService.getMetadata();

        return ResponseEntity.ok(
                ApiResponseFormat.success("상품 메타데이터 조회가 성공했습니다.", metadata)
        );
    }
}
