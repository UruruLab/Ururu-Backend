package com.ururulab.ururu.product.controller;

import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.ProductListResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductResponse;
import com.ururulab.ururu.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "상품", description = "상품 관련 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.ururulab.ururu.global.common.dto.ApiResponse<ProductResponse>> createProduct(
            @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "optionImages", required = false) List<MultipartFile> optionImages
    ) {
        ProductResponse response = productService.createProductWithImages(productRequest, optionImages);
        return ResponseEntity.ok(com.ururulab.ururu.global.common.dto.ApiResponse.success("상품이 등록되었습니다.", response));
    }

    @Operation(
            summary = "상품 목록 조회",
            description = "활성 상태의 상품 목록을 페이징하여 조회합니다. 카테고리 정보는 포함되지만 옵션과 정보고시는 제외됩니다. " +
                    "기본값: page=0, size=8, 정렬=생성일시+ID 내림차순"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<com.ururulab.ururu.global.common.dto.ApiResponse<Page<ProductListResponse>>> getProducts(
            @Parameter(description = "페이지 정보 (page, size, sort 파라미터)", example = "page=0&size=8&sort=createdAt,desc")
            @PageableDefault(page = 0, size = 10, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<ProductListResponse> products = productService.getProducts(pageable);
        return ResponseEntity.ok(com.ururulab.ururu.global.common.dto.ApiResponse.success("상품 목록 조회가 성공했습니다.", products));
    }

    @Operation(
            summary = "상품 상세 조회",
            description = "특정 상품의 상세 정보를 조회합니다. 카테고리, 옵션, 정보고시를 포함한 모든 데이터를 반환합니다. " +
                    "삭제된 옵션은 제외하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상품 ID"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<com.ururulab.ururu.global.common.dto.ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        ProductResponse product = productService.getProduct(productId);
        return ResponseEntity.ok(com.ururulab.ururu.global.common.dto.ApiResponse.success("상품 정보를 조회했습니다.", product));
    }

}
