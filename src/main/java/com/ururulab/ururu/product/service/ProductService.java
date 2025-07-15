package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.product.domain.entity.*;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.*;
import com.ururulab.ururu.product.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.dto.request.ProductNoticeRequest;
import com.ururulab.ururu.product.dto.request.ProductRequest;
import com.ururulab.ururu.product.dto.response.*;
import com.ururulab.ururu.product.event.ProductImageUploadEvent;
import com.ururulab.ururu.product.service.validation.ProductValidator;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductNoticeRepository productNoticeRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductValidator productValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final SellerRepository sellerRepository;
    private final ProductCategoryService productCategoryService;
    private final ProductTagService productTagService;
    private final ProductOptionService productOptionService;
    private final ProductOptionImageService productOptionImageService;

    /**
     * 상품 등록
     * @param productRequest
     * @param optionImages
     * @param sellerId
     * @return
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> optionImages, Long sellerId) {
        log.info("Creating product for seller: {}, productName: {}", sellerId, productRequest.name());

        // 1. 사전 검증 (트랜잭션 외부에서 수행)
        validateProductCreation(productRequest, optionImages, sellerId);

        // 2. 핵심 데이터 저장 (트랜잭션 내부)
        ProductCreationResult result = createProductCore(productRequest, sellerId);

        // 3. 비동기 이미지 업로드 이벤트 발행 (트랜잭션 외부)
        publishImageUploadEventIfNeeded(result.savedProduct(), result.savedOptions(), optionImages);

        // 4. 응답 생성
        ProductResponse response = createProductResponse(result);

        log.info("Product created successfully with ID: {} for seller: {}",
                result.savedProduct().getId(), sellerId);

        return response;
    }

    /**
     * 상품 목록 조회
     * @param pageable
     * @param sellerId
     * @return
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Pageable pageable, Long sellerId) {
        // 판매자 존재 확인
        validateSellerExists(sellerId);

        log.info("Getting product list for seller: {} - page: {}, size: {}, sort: {}",
                sellerId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        // 상품 목록 조회
        Page<Product> productPage = productRepository.findBySellerIdAndStatusIn(
                sellerId,
                Arrays.asList(Status.ACTIVE, Status.INACTIVE),
                pageable
        );

        if (productPage.isEmpty()) {
            log.info("No products found for seller: {}", sellerId);
            return Page.empty(pageable);
        }

        // 연관 데이터 배치 조회 및 응답 생성
        return buildProductListResponse(productPage, pageable);
    }

    /**
     * 상품 상세 조회
     * @param productId
     * @param sellerId
     * @return
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId, Long sellerId) {
        log.info("Getting product detail for ID: {} by seller: {}", productId, sellerId);

        // 기본 검증
        validateProductId(productId);

        // 상품 조회 (권한 포함)
        Product product = findProductByIdAndSeller(productId, sellerId);

        // 연관 데이터 조회 및 응답 생성
        return buildProductDetailResponse(product);
    }

    /**
     * 상품 수정 - 트랜잭션 범위 최적화
     * @param productId
     * @param productRequest
     * @param optionImages
     * @param sellerId
     * @return
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest productRequest,
                                         List<MultipartFile> optionImages, Long sellerId) {
        log.info("Updating product ID: {} for seller: {}", productId, sellerId);

        // 1. 사전 검증 (트랜잭션 외부)
        validateProductUpdate(productId, productRequest, optionImages, sellerId);

        // 2. 핵심 업데이트 수행 (트랜잭션 내부)
        ProductUpdateResult result = updateProductCore(productId, productRequest, optionImages, sellerId);

        // 3. 응답 생성
        ProductResponse response = buildProductUpdateResponse(result);

        log.info("Product update completed for ID: {}", productId);
        return response;
    }

    /**
     * 상품 삭제
     * @param productId
     * @param sellerId
     */
    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        log.info("Deleting product ID: {} for seller: {}", productId, sellerId);

        // 상품 조회 및 삭제 권한 검증
        Product product = validateAndGetProductForDeletion(productId, sellerId);

        // 삭제 처리
        product.updateStatus(Status.DELETED);
        productOptionRepository.markAllAsDeletedByProductId(productId);
        productRepository.save(product);

        log.info("Product deleted successfully: {}", productId);
    }

    /**
     * 상품 생성 사전 검증
     * @param productRequest
     * @param optionImages
     * @param sellerId
     */
    private void validateProductCreation(ProductRequest productRequest, List<MultipartFile> optionImages, Long sellerId) {
        // Seller 존재 확인
        validateSellerExists(sellerId);

        // 옵션-이미지 검증
        if (optionImages != null) {
            productValidator.validateOptionImagePair(productRequest.productOptions(), optionImages);
        }

        // 카테고리 및 태그 유효성 검증 (캐시된 검증)
        productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());
    }

    /**
     * 상품 수정 사전 검증
     * @param productId
     * @param productRequest
     * @param optionImages
     * @param sellerId
     */
    private void validateProductUpdate(Long productId, ProductRequest productRequest,
                                       List<MultipartFile> optionImages, Long sellerId) {
        validateProductId(productId);

        // 옵션-이미지 검증
        if (optionImages != null) {
            productValidator.validateOptionImagePair(productRequest.productOptions(), optionImages);
        }

        // 카테고리 및 태그 유효성 검증
        productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());
    }

    private void validateSellerExists(Long sellerId) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new BusinessException(SELLER_NOT_FOUND);
        }
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 상품 ID입니다.");
        }
    }

    /**
     * 핵심 상품 생성 로직 (트랜잭션 내부)
     * @param productRequest
     * @param sellerId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductCreationResult createProductCore(ProductRequest productRequest, Long sellerId) {
        log.info("Starting core product creation transaction for seller: {}", sellerId);

        // Seller 참조 (이미 존재 확인했으므로 안전)
        Seller seller = sellerRepository.getReferenceById(sellerId);

        // 카테고리 및 태그 조회
        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        List<TagCategory> tagCategories = productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());

        // 상품 저장
        Product savedProduct = productRepository.save(productRequest.toEntity(seller));

        // 연관 데이터 배치 저장
        productCategoryService.saveProductCategories(savedProduct, categories);
        List<ProductTag> savedProductTags = productTagService.saveProductTags(savedProduct, tagCategories);
        List<ProductOption> savedOptions = productOptionService.saveProductOptions(savedProduct, productRequest.productOptions());
        ProductNotice savedNotice = productNoticeRepository.save(productRequest.productNotice().toEntity(savedProduct));

        log.info("Core product creation completed for ID: {}", savedProduct.getId());

        return new ProductCreationResult(
                savedProduct,
                categories,
                savedOptions,
                savedNotice,
                savedProductTags
        );
    }

    /**
     * 핵심 상품 수정 로직 (트랜잭션 내부)
     * @param productId
     * @param productRequest
     * @param optionImages
     * @param sellerId
     * @return
     */
    @Transactional
    public ProductUpdateResult updateProductCore(Long productId, ProductRequest productRequest,
                                                 List<MultipartFile> optionImages, Long sellerId) {
        log.info("Starting core product update transaction for ID: {}", productId);

        // 기존 상품 조회
        Product existingProduct = findProductByIdAndSeller(productId, sellerId);

        // 카테고리 및 태그 조회
        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        List<TagCategory> tagCategories = productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());

        // 상품 기본 정보 업데이트
        boolean basicInfoChanged = updateProductBasicInfo(existingProduct, productRequest);
        if (basicInfoChanged) {
            productRepository.save(existingProduct);
        }

        // 연관 데이터 업데이트
        List<CategoryResponse> categoryResponses = productCategoryService.updateCategories(existingProduct, categories);
        List<ProductTagResponse> tagResponses = productTagService.updateTags(existingProduct, tagCategories);
        ProductNoticeResponse noticeResponse = updateNoticeFieldsIfChanged(existingProduct, productRequest.productNotice());
        List<ProductOptionResponse> optionResponses = productOptionService.updateOptions(existingProduct, productRequest.productOptions(), optionImages);

        log.info("Core product update completed for ID: {}", productId);

        return new ProductUpdateResult(
                existingProduct,
                categoryResponses,
                optionResponses,
                noticeResponse,
                tagResponses
        );
    }

    /**
     * 삭제를 위한 상품 검증 및 조회
     * @param productId
     * @param sellerId
     * @return
     */
    private Product validateAndGetProductForDeletion(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND));

        // 이미 삭제된 상품 확인
        if (product.getStatus() == Status.DELETED) {
            throw new BusinessException(PRODUCT_NOT_FOUND);
        }

        // 권한 확인
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ACCESS_DENIED);
        }

        // 삭제 가능 상태 확인
        if (!Arrays.asList(Status.ACTIVE, Status.INACTIVE).contains(product.getStatus())) {
            throw new BusinessException(ACCESS_DENIED);
        }

        return product;
    }

    /**
     * 상품 조회 (권한 포함)
     * @param productId
     * @param sellerId
     * @return
     */
    private Product findProductByIdAndSeller(Long productId, Long sellerId) {
        return productRepository.findByIdAndSellerIdAndStatusIn(
                productId,
                sellerId,
                Arrays.asList(Status.ACTIVE, Status.INACTIVE)
        ).orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND));
    }

    /**
     * 상품 목록 응답 생성
     * @param productPage
     * @param pageable
     * @return
     */
    private Page<ProductListResponse> buildProductListResponse(Page<Product> productPage, Pageable pageable) {
        List<Product> products = productPage.getContent();
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        // 배치 조회
        Map<Long, List<CategoryResponse>> categoriesMap =
                productCategoryService.getProductCategoriesBatch(productIds);
        Map<Long, List<ProductTagResponse>> tagsMap =
                productTagService.getProductTagsBatch(productIds);

        // 응답 생성
        List<ProductListResponse> content = products.stream()
                .map(product -> {
                    List<CategoryResponse> categories = categoriesMap
                            .getOrDefault(product.getId(), Collections.emptyList());
                    List<ProductTagResponse> tags = tagsMap
                            .getOrDefault(product.getId(), Collections.emptyList());
                    return ProductListResponse.from(product, categories, tags);
                })
                .toList();

        return new PageImpl<>(content, pageable, productPage.getTotalElements());
    }

    /**
     * 상품 상세 응답 생성
     * @param product
     * @return
     */
    private ProductResponse buildProductDetailResponse(Product product) {
        Long productId = product.getId();

        // 연관 데이터 조회
        List<CategoryResponse> categoryResponses = productCategoryRepository
                .findByProductIdWithCategory(productId).stream()
                .map(pc -> CategoryResponse.from(pc.getCategory()))
                .toList();

        List<ProductOptionResponse> optionResponses = productOptionRepository
                .findByProductIdAndIsDeletedFalse(productId).stream()
                .map(ProductOptionResponse::from)
                .toList();

        ProductNoticeResponse noticeResponse = productNoticeRepository
                .findByProductId(productId)
                .map(ProductNoticeResponse::from)
                .orElseThrow(() -> new IllegalStateException("상품 정보고시가 존재하지 않습니다."));

        List<ProductTagResponse> tagResponses = productTagRepository
                .findByProductIdsWithTagCategory(Collections.singletonList(productId)).stream()
                .map(ProductTagResponse::from)
                .toList();

        return ProductResponse.from(product, categoryResponses, optionResponses, noticeResponse, tagResponses);
    }

    /**
     * 필요한 경우 이미지 업로드 이벤트 발행
     * @param savedProduct
     * @param savedOptions
     * @param optionImages
     */
    private void publishImageUploadEventIfNeeded(Product savedProduct, List<ProductOption> savedOptions,
                                                 List<MultipartFile> optionImages) {
        if (optionImages != null && !optionImages.isEmpty()) {
            List<ProductImageUploadRequest> uploadRequests =
                    productOptionImageService.createImageUploadRequests(savedOptions, optionImages);

            eventPublisher.publishEvent(new ProductImageUploadEvent(savedProduct.getId(), uploadRequests));
            log.info("Image upload event published for product: {}", savedProduct.getId());
        }
    }

    /**
     * 상품 생성 응답 생성
     * @param result
     * @return
     */
    private ProductResponse createProductResponse(ProductCreationResult result) {
        List<CategoryResponse> categoryResponses = result.categories().stream()
                .map(CategoryResponse::from)
                .toList();

        List<ProductOptionResponse> optionResponses = result.savedOptions().stream()
                .map(ProductOptionResponse::from)
                .toList();

        ProductNoticeResponse noticeResponse = ProductNoticeResponse.from(result.savedNotice());

        List<ProductTagResponse> productTagResponses = result.savedProductTags().stream()
                .map(ProductTagResponse::from)
                .toList();

        return ProductResponse.from(
                result.savedProduct(),
                categoryResponses,
                optionResponses,
                noticeResponse,
                productTagResponses
        );
    }

    /**
     * 상품 수정 응답 생성
     * @param result
     * @return
     */
    private ProductResponse buildProductUpdateResponse(ProductUpdateResult result) {
        return ProductResponse.from(
                result.product(),
                result.categoryResponses(),
                result.optionResponses(),
                result.noticeResponse(),
                result.tagResponses()
        );
    }

    /**
     * 상품 기본 정보 업데이트 (변경된 필드만)
     * @param product
     * @param request
     * @return
     */
    private boolean updateProductBasicInfo(Product product, ProductRequest request) {
        boolean changed = false;

        if (!request.name().equals(product.getName())) {
            product.updateName(request.name());
            changed = true;
            log.info("Product name updated: '{}' -> '{}' for ID: {}", product.getName(), request.name(), product.getId());
        }

        if (!request.description().equals(product.getDescription())) {
            product.updateDescription(request.description());
            changed = true;
            log.info("Product description updated for ID: {}", product.getId());
        }

        return changed;
    }

    /**
     * 정보고시 업데이트
     * @param product
     * @param noticeRequest
     * @return
     */
    private ProductNoticeResponse updateNoticeFieldsIfChanged(Product product, ProductNoticeRequest noticeRequest) {
        ProductNotice existingNotice = productNoticeRepository
                .findByProductId(product.getId())
                .orElseThrow(() -> new IllegalStateException("상품 정보고시가 존재하지 않습니다."));

        boolean changed = false;

        // 각 필드별로 변경 여부 확인하고 변경된 것만 UPDATE
        if (!noticeRequest.capacity().equals(existingNotice.getCapacity())) {
            existingNotice.updateCapacity(noticeRequest.capacity());
            changed = true;
        }

        if (!noticeRequest.spec().equals(existingNotice.getSpec())) {
            existingNotice.updateSpec(noticeRequest.spec());
            changed = true;
        }

        if (!noticeRequest.expiry().equals(existingNotice.getExpiry())) {
            existingNotice.updateExpiry(noticeRequest.expiry());
            changed = true;
        }

        if (!noticeRequest.usage().equals(existingNotice.getUsage())) {
            existingNotice.updateUsage(noticeRequest.usage());
            changed = true;
        }

        if (!noticeRequest.manufacturer().equals(existingNotice.getManufacturer())) {
            existingNotice.updateManufacturer(noticeRequest.manufacturer());
            changed = true;
        }

        if (!noticeRequest.responsibleSeller().equals(existingNotice.getResponsibleSeller())) {
            existingNotice.updateResponsibleSeller(noticeRequest.responsibleSeller());
            changed = true;
        }

        if (!noticeRequest.countryOfOrigin().equals(existingNotice.getCountryOfOrigin())) {
            existingNotice.updateCountryOfOrigin(noticeRequest.countryOfOrigin());
            changed = true;
        }

        if (!noticeRequest.functionalCosmetics().equals(existingNotice.getFunctionalCosmetics())) {
            existingNotice.updateFunctionalCosmetics(noticeRequest.functionalCosmetics());
            changed = true;
        }

        if (!noticeRequest.caution().equals(existingNotice.getCaution())) {
            existingNotice.updateCaution(noticeRequest.caution());
            changed = true;
        }

        if (!noticeRequest.warranty().equals(existingNotice.getWarranty())) {
            existingNotice.updateWarranty(noticeRequest.warranty());
            changed = true;
        }

        if (!noticeRequest.customerServiceNumber().equals(existingNotice.getCustomerServiceNumber())) {
            existingNotice.updateCustomerServiceNumber(noticeRequest.customerServiceNumber());
            changed = true;
        }

        if (changed) {
            ProductNotice savedNotice = productNoticeRepository.save(existingNotice);
            log.info("Product notice updated for product: {}", product.getId());
            return ProductNoticeResponse.from(savedNotice);
        } else {
            log.info("Product notice unchanged for product: {}", product.getId());
            return ProductNoticeResponse.from(existingNotice);
        }
    }

    /**
     * 상품 생성 결과를 담는 record
     * @param savedProduct
     * @param categories
     * @param savedOptions
     * @param savedNotice
     * @param savedProductTags
     */
    public record ProductCreationResult(
            Product savedProduct,
            List<Category> categories,
            List<ProductOption> savedOptions,
            ProductNotice savedNotice,
            List<ProductTag> savedProductTags
    ) {}

    /**
     * 상품 수정 결과를 담는 record
     * @param product
     * @param categoryResponses
     * @param optionResponses
     * @param noticeResponse
     * @param tagResponses
     */
    public record ProductUpdateResult(
            Product product,
            List<CategoryResponse> categoryResponses,
            List<ProductOptionResponse> optionResponses,
            ProductNoticeResponse noticeResponse,
            List<ProductTagResponse> tagResponses
    ) {}
}
