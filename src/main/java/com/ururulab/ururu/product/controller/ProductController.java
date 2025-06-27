package com.ururulab.ururu.product.controller;

import com.ururulab.ururu.global.common.dto.ApiResponseFormat;
import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.ProductListResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductResponse;
import com.ururulab.ururu.product.service.ProductService;
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

@Tag(name = "상품", description = "상품 관리 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/products/{sellerId}")  // sellerId를 경로에 포함
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "상품 등록",
            description = "판매자가 새로운 상품을 등록합니다. 상품 정보와 옵션 이미지를 함께 업로드할 수 있습니다. " +
                    "이미지는 비동기로 처리되어 즉시 응답을 받을 수 있습니다. " +
                    "상품 정보는 JSON 형태로, 이미지는 MultipartFile 배열로 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (필수 필드 누락, 유효하지 않은 카테고리 등)"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "413", description = "업로드 파일 크기 초과"),
            @ApiResponse(responseCode = "415", description = "지원하지 않는 파일 형식"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseFormat<ProductResponse>> createProduct(
            @Parameter(description = "판매자 ID", example = "1", required = true)
            @PathVariable Long sellerId,

            @Parameter(
                    description = "상품 등록 정보 (JSON 형태)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductRequest.class)
                    )
            )
            @Valid @RequestPart("product") ProductRequest productRequest,

            @Parameter(
                    description = "상품 옵션별 이미지 파일들 (선택사항). 옵션 순서와 이미지 순서가 일치해야 합니다.",
                    required = false,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "array", format = "binary")
                    )
            )
            @RequestPart(value = "optionImages", required = false) List<MultipartFile> optionImages
    ) {
        ProductResponse response = productService.createProduct(productRequest, optionImages, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("상품이 등록되었습니다.", response));
    }

    @Operation(
            summary = "판매자 상품 목록 조회",
            description = "특정 판매자의 상품 목록을 페이징하여 조회합니다. 카테고리 정보는 포함되지만 옵션과 정보고시는 제외됩니다. " +
                    "기본값: page=0, size=10, 정렬=생성일시+ID 내림차순"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 판매자"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponseFormat<Page<ProductListResponse>>> getProducts(
            @Parameter(description = "판매자 ID", example = "1", required = true)
            @PathVariable Long sellerId,

            @Parameter(description = "페이지 정보 (page, size, sort 파라미터)", example = "page=0&size=10&sort=createdAt,desc")
            @PageableDefault(page = 0, size = 10, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<ProductListResponse> products = productService.getProducts(pageable, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품 목록 조회가 성공했습니다.", products));
    }

    @Operation(
            summary = "상품 상세 조회",
            description = "판매자의 특정 상품 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품 또는 판매자"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponseFormat<ProductResponse>> getProduct(
            @Parameter(description = "판매자 ID", example = "1", required = true)
            @PathVariable Long sellerId,

            @Parameter(description = "조회할 상품의 ID", example = "123", required = true)
            @PathVariable Long productId
    ) {
        ProductResponse response = productService.getProduct(productId, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품 상세 조회가 성공했습니다.", response));
    }
}
