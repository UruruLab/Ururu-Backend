package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.product.domain.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.domain.dto.request.ProductNoticeRequest;
import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.*;
import com.ururulab.ururu.product.domain.entity.*;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.*;
import com.ururulab.ururu.product.event.ProductImageUploadEvent;
import com.ururulab.ururu.product.service.validation.ProductValidator;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

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
     * 상품 등록 - 비동기 이미지 업로드 (sellerId 추가)
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> optionImages, Long sellerId) {
        StopWatch stopWatch = new StopWatch("ProductCreation");
        stopWatch.start("validation");

        log.info("Creating product for seller: {}, productName: {}", sellerId, productRequest.name());

        // 1. Seller 조회 추가
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(SELLER_NOT_FOUND));

        if (optionImages != null) {
            productValidator.validateOptionImagePair(productRequest.productOptions(), optionImages);
        }

        stopWatch.stop();

        // 3. 카테고리 및 태그카테고리 유효성 검증
        stopWatch.start("categoryValidation");
        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        List<TagCategory> tagCategories = productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());
        stopWatch.stop();

        // 4. 상품 저장 (Seller와 함께)
        stopWatch.start("productSave");
        Product savedProduct = productRepository.save(productRequest.toEntity(seller)); // seller 전달
        stopWatch.stop();

        // 5. 배치로 연관 데이터 저장 - 기존과 동일
        stopWatch.start("relatedDataSave");
        productCategoryService.saveProductCategories(savedProduct, categories);
        List<ProductTag> savedProductTags = productTagService.saveProductTags(savedProduct, tagCategories);
        List<ProductOption> savedOptions = productOptionService.saveProductOptions(savedProduct, productRequest.productOptions());
        ProductNotice savedNotice = productNoticeRepository.save(productRequest.productNotice().toEntity(savedProduct));
        stopWatch.stop();

        // 6. 비동기 이미지 업로드 이벤트 발행 - 기존과 동일
        if (optionImages != null && !optionImages.isEmpty()) {
            stopWatch.start("eventPublish");
            List<ProductImageUploadRequest> uploadRequests = productOptionImageService.createImageUploadRequests(savedOptions, optionImages);
            eventPublisher.publishEvent(new ProductImageUploadEvent(savedProduct.getId(), uploadRequests));
            stopWatch.stop();
        }

        // 7. 응답 생성 - 기존과 동일
        stopWatch.start("responseCreation");
        ProductResponse response = createProductResponseOptimized(savedProduct, categories, savedOptions, savedNotice, savedProductTags);
        stopWatch.stop();

        log.info("Product created successfully with ID: {} for seller: {} | Performance: {}",
                savedProduct.getId(), sellerId, stopWatch.prettyPrint());

        return response;
    }

    /**
     * 상품 목록 조회 (sellerId 추가)
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Pageable pageable, Long sellerId) {
        sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));


        StopWatch stopWatch = new StopWatch("ProductListRetrieval");
        stopWatch.start("productQuery");

        log.info("Getting product list for seller: {} - page: {}, size: {}, sort: {}",
                sellerId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        // 1. 특정 판매자의 상품 목록 조회 (기존: 모든 상품 → 변경: 특정 판매자만)
        Page<Product> productPage = productRepository.findBySellerIdAndStatusIn(
                sellerId,  // sellerId 추가
                Arrays.asList(Status.ACTIVE, Status.INACTIVE),
                pageable
        );
        stopWatch.stop();

        if (productPage.isEmpty()) {
            log.info("No products found for seller: {}", sellerId);
            return Page.empty(pageable);
        }

        // 2. 카테고리 및 태그 배치 조회 - 기존과 동일
        stopWatch.start("categoryBatchQuery");
        List<Product> products = productPage.getContent();
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<CategoryResponse>> categoriesMap =
                productCategoryService.getProductCategoriesBatch(productIds);
        stopWatch.stop();

        stopWatch.start("tagBatchQuery");
        // ProductService getProducts 메서드에서
        Map<Long, List<ProductTagResponse>> tagsMap =
                productTagService.getProductTagsBatch(productIds);
        stopWatch.stop();

        // 3. 응답 생성 - 기존과 동일
        stopWatch.start("responseCreation");
        List<ProductListResponse> content = products.stream()
                .map(product -> {
                    List<CategoryResponse> categories = categoriesMap
                            .getOrDefault(product.getId(), Collections.emptyList());

                    List<ProductTagResponse> tags = tagsMap
                            .getOrDefault(product.getId(), Collections.emptyList());

                    return ProductListResponse.from(product, categories, tags);
                })
                .toList();

        Page<ProductListResponse> result = new PageImpl<>(content, pageable, productPage.getTotalElements());
        stopWatch.stop();

        log.info("Product list retrieved successfully for seller: {} - {} products | Performance: {}",
                sellerId, content.size(), stopWatch.prettyPrint());

        return result;
    }

    /**
     * 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId, Long sellerId) {
        StopWatch stopWatch = new StopWatch("validate");

        log.info("Getting product detail for ID: {} by seller: {}", productId, sellerId);

        // 1. 기본 검증
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 상품 ID입니다.");
        }

        stopWatch.start("productQuery");
        // 2. 상품 조회 (해당 판매자의 상품만 + DELETED 제외)
        Product product = productRepository.findByIdAndSellerIdAndStatusIn(
                productId,
                sellerId,
                Arrays.asList(Status.ACTIVE, Status.INACTIVE)
        ).orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND));
        stopWatch.stop();

        stopWatch.start("categoryQuery");
        // 3. 카테고리 조회
        List<CategoryResponse> categoryResponses = productCategoryRepository
                .findByProductIdWithCategory(productId).stream()
                .map(pc -> CategoryResponse.from(pc.getCategory()))
                .toList();
        stopWatch.stop();

        stopWatch.start("optionQuery");
        // 4. 옵션 조회 (삭제되지 않은 것만)
        List<ProductOptionResponse> optionResponses = productOptionRepository
                .findByProductIdAndIsDeletedFalse(productId).stream()
                .map(ProductOptionResponse::from)
                .toList();
        stopWatch.stop();

        stopWatch.start("noticeQuery");
        // 5. 정보고시 조회
        ProductNoticeResponse noticeResponse = productNoticeRepository
                .findByProductId(productId)
                .map(ProductNoticeResponse::from)
                .orElseThrow(() -> new IllegalStateException("상품 정보고시가 존재하지 않습니다."));
        stopWatch.stop();

        stopWatch.start("tagQuery");
        // 6. 태그 조회
        List<ProductTagResponse> tagResponses = productTagRepository
                .findByProductIdsWithTagCategory(Collections.singletonList(productId)).stream()
                .map(ProductTagResponse::from)
                .toList();
        stopWatch.stop();

        stopWatch.start("responseCreation");

        // 7. 응답 생성
        ProductResponse response = ProductResponse.from(
                product,
                categoryResponses,
                optionResponses,
                noticeResponse,
                tagResponses
        );
        stopWatch.stop();

        log.info("Product detail retrieved for productId: {}, sellerId: {} | Performance: {}",
                productId, sellerId, stopWatch.prettyPrint());
        return response;
    }

    private ProductResponse createProductResponseOptimized(
            Product savedProduct,
            List<Category> categories,
            List<ProductOption> savedOptions,
            ProductNotice savedNotice,
            List<ProductTag> savedProductTags) {

        List<CategoryResponse> categoryResponses = categories.stream()
                .map(CategoryResponse::from)
                .toList();

        List<ProductOptionResponse> optionResponses = savedOptions.stream()
                .map(ProductOptionResponse::from)
                .toList();

        ProductNoticeResponse noticeResponse = ProductNoticeResponse.from(savedNotice);

        List<ProductTagResponse> productTagResponses = savedProductTags.stream()
                .map(ProductTagResponse::from)
                .toList();

        return ProductResponse.from(savedProduct, categoryResponses, optionResponses, noticeResponse, productTagResponses);
    }

    /**
     * 상품 수정 - 변경된 부분만 UPDATE + 이미지 해시 비교
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest productRequest,
                                         List<MultipartFile> optionImages, Long sellerId) {
        StopWatch stopWatch = new StopWatch("ProductUpdate");
        stopWatch.start("validation");

        log.info("Updating product ID: {} for seller: {}", productId, sellerId);

        // 1. 기본 검증 및 기존 상품 조회
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 상품 ID입니다.");
        }

        Product existingProduct = productRepository.findByIdAndSellerIdAndStatusIn(
                productId, sellerId, Arrays.asList(Status.ACTIVE, Status.INACTIVE)
        ).orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND));


        // 2. 옵션-이미지 검증
        if (optionImages != null) {
            productValidator.validateOptionImagePair(productRequest.productOptions(), optionImages);
        }

        stopWatch.stop();

        // 3. 카테고리 및 태그카테고리 유효성 검증 (기존과 동일)
        stopWatch.start("categoryValidation");
        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        List<TagCategory> tagCategories = productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());
        stopWatch.stop();

        // 4. 상품 기본 정보 업데이트 (변경된 경우만)
        stopWatch.start("basicInfoUpdate");
        boolean basicInfoChanged = updateProductBasicInfo(existingProduct, productRequest);
        if (basicInfoChanged) {
            productRepository.save(existingProduct);
            log.info("Product basic info updated for ID: {}", productId);
        } else {
            log.info("Product basic info unchanged for ID: {}", productId);
        }
        stopWatch.stop();

        // 5. 카테고리 업데이트 (변경된 것만)
        stopWatch.start("categoryUpdate");
        List<CategoryResponse> categoryResponses = productCategoryService.updateCategories(existingProduct, categories);
        stopWatch.stop();

        // 6. 태그 업데이트 (변경된 것만)
        stopWatch.start("tagUpdate");
        List<ProductTagResponse> tagResponses = productTagService.updateTags(existingProduct, tagCategories);
        stopWatch.stop();

        // 7. 정보고시 업데이트 (변경된 필드만)
        stopWatch.start("noticeUpdate");
        ProductNoticeResponse noticeResponse = updateNoticeFieldsIfChanged(existingProduct, productRequest.productNotice());
        stopWatch.stop();

        // 8. 옵션 업데이트 (변경된 것만 + 이미지 해시 비교)
        stopWatch.start("optionUpdate");
        List<ProductOptionResponse> optionResponses = productOptionService.updateOptions(existingProduct, productRequest.productOptions(), optionImages);
        stopWatch.stop();

        // 9. 응답 생성 (기존과 동일)
        stopWatch.start("responseCreation");
        ProductResponse response = ProductResponse.from(existingProduct, categoryResponses, optionResponses, noticeResponse, tagResponses);
        stopWatch.stop();

        log.info("Product update completed for ID: {} | Performance: {}", productId, stopWatch.prettyPrint());
        return response;
    }

    /**
     * 상품 기본 정보 업데이트 (변경된 필드만)
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
            log.info("Notice capacity updated for product: {}", product.getId());
        }

        if (!noticeRequest.spec().equals(existingNotice.getSpec())) {
            existingNotice.updateSpec(noticeRequest.spec());
            changed = true;
            log.info("Notice spec updated for product: {}", product.getId());
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

    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        // 1. 상품 존재 여부 먼저 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 상품 상태 확인 (이미 삭제된 상품)
        if (product.getStatus() == Status.DELETED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 3. 권한 확인
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4. 상태 확인 (ACTIVE, INACTIVE만 삭제 가능)
        if (!Arrays.asList(Status.ACTIVE, Status.INACTIVE).contains(product.getStatus())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 5. 삭제 처리
        product.updateStatus(Status.DELETED);
        productOptionRepository.markAllAsDeletedByProductId(productId);
        productRepository.save(product);
    }
}
