package com.ururulab.ururu.product.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
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

    /**
     * 상품 수정
     */
    @Operation(
            summary = "상품 수정",
            description = "판매자가 기존 상품 정보를 수정합니다. 기존 데이터와 비교하여 변경된 부분만 업데이트됩니다. " +
                    "- 상품 기본 정보: 변경된 필드만 업데이트 " +
                    "- 카테고리/태그: 변경된 경우에만 삭제 후 재생성 " +
                    "- 옵션: ID가 있으면 기존 옵션 업데이트, 없으면 새 옵션 생성 " +
                    "- 이미지: 새 파일이 업로드된 경우에만 S3 업로드, 기존 이미지는 자동 삭제 " +
                    "- 정보고시: 변경된 필드만 업데이트 " +
                    "프론트엔드에서는 등록과 동일한 형태로 전체 데이터를 전송하면 됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (필수 필드 누락, 유효하지 않은 카테고리 등)"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (다른 판매자의 상품)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품 또는 삭제된 상품"),
            @ApiResponse(responseCode = "413", description = "업로드 파일 크기 초과"),
            @ApiResponse(responseCode = "415", description = "지원하지 않는 파일 형식"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseFormat<ProductResponse>> updateProduct(
            @Parameter(description = "판매자 ID", example = "1", required = true)
            @PathVariable Long sellerId,

            @Parameter(description = "수정할 상품 ID", example = "123", required = true)
            @PathVariable Long productId,

            @Parameter(
                    description = "상품 수정 정보 (JSON 형태). 등록과 동일한 구조를 사용합니다. " +
                            "기존 옵션 수정 시 id 필드를 포함하고, 새 옵션 추가 시 id는 null로 설정합니다. " +
                            "변경되지 않은 필드도 기존 값을 포함하여 전송해야 합니다.",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductRequest.class)
                    )
            )
            @Valid @RequestPart("product") ProductRequest productRequest,

            @Parameter(
                    description = "상품 옵션별 이미지 파일들 (선택사항). " +
                            "옵션 순서와 이미지 순서가 일치해야 합니다. " +
                            "이미지를 변경하지 않을 옵션은 해당 인덱스에 파일을 전송하지 않으면 기존 이미지가 유지됩니다. " +
                            "새 이미지를 업로드하면 기존 이미지는 자동으로 삭제됩니다. " +
                            "서버에서 자동으로 변경 감지하므로 동일한 이미지를 다시 업로드할 필요가 없습니다.",
                    required = false,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "array", format = "binary")
                    )
            )
            @RequestPart(value = "optionImages", required = false) List<MultipartFile> optionImages
    ) {
        ProductResponse response = productService.updateProduct(productId, productRequest, optionImages, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("상품이 수정되었습니다.", response));
    }

}
