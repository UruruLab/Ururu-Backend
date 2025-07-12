package com.ururulab.ururu.product.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.groupBuy.util.AuthUtils;
import com.ururulab.ururu.product.dto.request.ProductRequest;
import com.ururulab.ururu.product.dto.response.ProductListResponse;
import com.ururulab.ururu.product.dto.response.ProductOptionResponse;
import com.ururulab.ururu.product.dto.response.ProductResponse;
import com.ururulab.ururu.product.service.ProductOptionService;
import com.ururulab.ururu.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductOptionService productOptionService;

    @Operation(summary = "상품 등록", description = "판매자가 새로운 상품을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품이 등록되었습니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 판매자입니다."),
            @ApiResponse(responseCode = "413", description = "파일 크기가 제한을 초과했습니다."),
            @ApiResponse(responseCode = "415", description = "옵션 n번째 이미지가 유효하지 않습니다: 지원하지 않는 확장자: pdf"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseFormat<ProductResponse>> createProduct(
            @Valid @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "optionImages", required = false) List<MultipartFile> optionImages
    ) {
        Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        ProductResponse response = productService.createProduct(productRequest, optionImages, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("상품이 등록되었습니다.", response));
    }

    @Operation(summary = "상품 수정", description = "판매자가 기존 상품 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 수정 성공"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 상품"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식"),
            @ApiResponse(responseCode = "413", description = "파일 크기 초과"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseFormat<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "optionImages", required = false) List<MultipartFile> optionImages
    ) {
        Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        ProductResponse response = productService.updateProduct(productId, productRequest, optionImages, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품이 수정되었습니다.", response));
    }

    @Operation(summary = "판매자 상품 목록 조회", description = "판매자의 상품 목록을 페이지네이션 형태로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 판매자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponseFormat<Page<ProductListResponse>>> getProducts(
            @PageableDefault(page = 0, size = 10, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        Page<ProductListResponse> products = productService.getProducts(pageable, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품 목록 조회가 성공했습니다.", products));
    }

    @Operation(summary = "상품 상세 조회", description = "판매자의 특정 상품 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 상품"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품 또는 판매자"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponseFormat<ProductResponse>> getProduct(
            @PathVariable Long productId
    ) {
        Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        ProductResponse response = productService.getProduct(productId, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품 상세 조회가 성공했습니다.", response));
    }

    @Operation(summary = "특정 상품의 옵션 조회", description = "판매자가 특정 상품의 옵션을 조회 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 옵션 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 옵션"),
    })
    @GetMapping("/{productId}/options")
    public ResponseEntity<ApiResponseFormat<List<ProductOptionResponse>>> getProductOptions(
            @PathVariable Long productId
    ) {

        Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        List<ProductOptionResponse> response = productOptionService.getProductOptions(productId, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품 옵션 조회가 성공했습니다.", response));
    }

    @Operation(summary = "상품 옵션 삭제", description = "판매자가 특정 상품의 옵션을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 옵션 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "상품의 마지막 옵션은 삭제할 수 없음"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 옵션"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{productId}/options/{optionId}")
    public ResponseEntity<ApiResponseFormat<Void>> deleteProductOption(
            @PathVariable Long productId,
            @PathVariable Long optionId
    ) {
        Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        productOptionService.deleteProductOption(productId, optionId, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품 옵션이 삭제되었습니다."));
    }

    @Operation(summary = "상품 삭제", description = "판매자가 상품을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponseFormat<Void>> deleteProduct(
            @PathVariable Long productId
    ) {
        Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        productService.deleteProduct(productId, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품이 삭제되었습니다."));
    }
}
