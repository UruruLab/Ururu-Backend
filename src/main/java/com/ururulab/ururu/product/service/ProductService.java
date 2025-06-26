package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.common.entity.TagCategory;
import com.ururulab.ururu.product.domain.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.*;
import com.ururulab.ururu.product.domain.entity.*;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.*;
import com.ururulab.ururu.product.service.validation.ProductValidator;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductNoticeRepository productNoticeRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductValidator productValidator;
    private final ProductOptionImageService productOptionImageService;

    /**
     * 상품 등록 (이미지 없음)
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        return createProductWithImages(productRequest, null);
    }

//    /**
//     * 상품 등록 (이미지 포함)
//     */
//    @Transactional
//    public ProductResponse createProductWithImages(ProductRequest productRequest, List<MultipartFile> optionImages) {
//        StopWatch stopWatch = new StopWatch("ProductCreation");
//        stopWatch.start("validation");
//
//        log.info("Creating product with images: {}", productRequest.name());
//
//        // 1. 요청 데이터 검증
//        productValidator.validateProductRequest(productRequest);
//        validateImageCount(productRequest.productOptions(), optionImages);
//
//        // 2. 이미지 사전 검증 (업로드 전에 모든 이미지 검증)
//        validateAllImages(optionImages);
//        stopWatch.stop();
//
//        // 3. 카테고리 유효성 검증
//        stopWatch.start("categoryValidation");
//        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
//        stopWatch.stop();
//
//        // 4. 상품 저장
//        stopWatch.start("productSave");
//        Product savedProduct = productRepository.save(productRequest.toEntity());
//        stopWatch.stop();
//
//        // 5. 배치로 연관 데이터 저장
//        stopWatch.start("relatedDataSave");
//        saveProductCategoriesBatch(savedProduct, categories);
//        List<ProductOption> savedOptions = saveProductOptionsBatch(savedProduct, productRequest.productOptions());
//        ProductNotice savedNotice = productNoticeRepository.save(productRequest.productNotice().toEntity(savedProduct));
//        stopWatch.stop();
//
//        // 6. 이미지 업로드 및 URL 업데이트 (동기 처리)
//        stopWatch.start("imageUpload");
//        uploadAndUpdateProductOptionImages(savedOptions, optionImages);
//        stopWatch.stop();
//
//        // 7. 응답 생성
//        stopWatch.start("responseCreation");
//        ProductResponse response = createProductResponseOptimized(savedProduct, categories, savedOptions, savedNotice);
//        stopWatch.stop();
//
//        log.info("Product created successfully with ID: {} | Performance: {}",
//                savedProduct.getId(), stopWatch.prettyPrint());
//
//        return response;
//    }
//
//    /**
//     * 이미지 개수 검증
//     */
//    private void validateImageCount(List<ProductOptionRequest> options, List<MultipartFile> images) {
//        if (images == null || images.isEmpty()) {
//            return; // 이미지가 없어도 OK
//        }
//
//        if (images.size() != options.size()) {
//            throw new IllegalArgumentException(
//                    String.format("옵션 개수(%d)와 이미지 개수(%d)가 일치하지 않습니다.", options.size(), images.size())
//            );
//        }
//    }
//
//    /**
//     * 모든 이미지 사전 검증
//     */
//    private void validateAllImages(List<MultipartFile> optionImages) {
//        if (optionImages == null || optionImages.isEmpty()) {
//            return;
//        }
//
//        for (int i = 0; i < optionImages.size(); i++) {
//            MultipartFile imageFile = optionImages.get(i);
//
//            // 빈 파일은 스킵
//            if (imageFile == null || imageFile.isEmpty()) {
//                continue;
//            }
//
//            try {
//                // 이미지 검증만 수행 (업로드는 하지 않음)
//                productOptionImageService.validateImage(imageFile);
//            } catch (Exception e) {
//                throw new IllegalArgumentException(
//                        String.format("옵션 %d번째 이미지가 유효하지 않습니다: %s", i + 1, e.getMessage()), e
//                );
//            }
//        }
//    }
//
//    /**
//     * 상품 옵션 이미지 업로드 및 DB 업데이트
//     */
//    private void uploadAndUpdateProductOptionImages(List<ProductOption> savedOptions, List<MultipartFile> optionImages) {
//        if (optionImages == null || optionImages.isEmpty()) {
//            return;
//        }
//
//        for (int i = 0; i < savedOptions.size() && i < optionImages.size(); i++) {
//            MultipartFile imageFile = optionImages.get(i);
//
//            // 빈 파일은 스킵
//            if (imageFile == null || imageFile.isEmpty()) {
//                continue;
//            }
//
//            ProductOption option = savedOptions.get(i);
//
//            try {
//                // 이미지 업로드 (이미 검증된 이미지)
//                String imageUrl = productOptionImageService.uploadProductOptionImage(imageFile);
//
//                // DB 업데이트
//                option.updateImageUrl(imageUrl);
//                productOptionRepository.save(option);
//
//                log.info("Image uploaded for option ID: {} -> {}", option.getId(), imageUrl);
//
//            } catch (Exception e) {
//                log.error("Failed to upload image for option index {}: {}", i, e.getMessage());
//                // 상품 옵션 이미지 업로드 실패시 전체 트랜잭션 롤백
//                throw new RuntimeException(
//                        String.format("옵션 %d번째 이미지 업로드에 실패했습니다: %s", i + 1, e.getMessage()), e
//                );
//            }
//        }
//    }
//
//    /**
//     * 배치로 상품-카테고리 연관관계 저장
//     */
//    private void saveProductCategoriesBatch(Product product, List<Category> categories) {
//        if (categories.isEmpty()) return;
//
//        List<ProductCategory> productCategories = categories.stream()
//                .map(category -> ProductCategory.of(product, category))
//                .toList();
//
//        productCategoryRepository.saveAll(productCategories);
//    }
//
//    /**
//     * 배치로 상품 옵션 저장
//     */
//    private List<ProductOption> saveProductOptionsBatch(Product product, List<ProductOptionRequest> requests) {
//        if (requests.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<ProductOption> productOptions = requests.stream()
//                .map(request -> request.toEntity(product))
//                .toList();
//
//        // saveAll 후 ID가 할당된 Entity 반환
//        return productOptionRepository.saveAll(productOptions);
//    }
//
//    /**
//     * 응답 생성 최적화
//     */
//    private ProductResponse createProductResponseOptimized(
//            Product savedProduct,
//            List<Category> categories,
//            List<ProductOption> savedOptions,  // insert 후 Entity 활용
//            ProductNotice savedNotice) {
//
//        List<CategoryResponse> categoryResponses = categories.stream()
//                .map(CategoryResponse::from)
//                .toList();
//
//        // insert 후 반환된 Entity 활용
//        List<ProductOptionResponse> optionResponses = savedOptions.stream()
//                .map(ProductOptionResponse::from)
//                .toList();
//
//        ProductNoticeResponse noticeResponse = ProductNoticeResponse.from(savedNotice);
//
//        return ProductResponse.from(savedProduct, categoryResponses, optionResponses, noticeResponse);
//    }

    /**
     * 상품 등록 (이미지 포함) - ProductTag 추가
     */
    @Transactional
    public ProductResponse createProductWithImages(ProductRequest productRequest, List<MultipartFile> optionImages) {
        StopWatch stopWatch = new StopWatch("ProductCreation");
        stopWatch.start("validation");

        log.info("Creating product with images: {}", productRequest.name());

        // 1. 요청 데이터 검증
        productValidator.validateProductRequest(productRequest);
        validateImageCount(productRequest.productOptions(), optionImages);

        // 2. 이미지 사전 검증 (업로드 전에 모든 이미지 검증)
        validateAllImages(optionImages);
        stopWatch.stop();

        // 3. 카테고리 및 태그카테고리 유효성 검증
        stopWatch.start("categoryAndTagValidation");
        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        List<TagCategory> tagCategories = productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());
        stopWatch.stop();

        // 4. 상품 저장
        stopWatch.start("productSave");
        Product savedProduct = productRepository.save(productRequest.toEntity());
        stopWatch.stop();

        // 5. 배치로 연관 데이터 저장 (저장된 Entity 반환받기)
        stopWatch.start("relatedDataSave");
        saveProductCategoriesBatch(savedProduct, categories);
        List<ProductTag> savedProductTags = saveProductTagsBatch(savedProduct, tagCategories); // ID 포함해서 반환
        List<ProductOption> savedOptions = saveProductOptionsBatch(savedProduct, productRequest.productOptions());
        ProductNotice savedNotice = productNoticeRepository.save(productRequest.productNotice().toEntity(savedProduct));
        stopWatch.stop();

        // 6. 이미지 업로드 및 URL 업데이트 (동기 처리)
        stopWatch.start("imageUpload");
        uploadAndUpdateProductOptionImages(savedOptions, optionImages);
        stopWatch.stop();

        // 7. 응답 생성 (저장된 ProductTag 사용)
        stopWatch.start("responseCreation");
        ProductResponse response = createProductResponseOptimized(savedProduct, categories, savedOptions, savedNotice, savedProductTags);
        stopWatch.stop();

        log.info("Product created successfully with ID: {} | Performance: {}",
                savedProduct.getId(), stopWatch.prettyPrint());

        return response;
    }

    /**
     * 배치로 상품-태그카테고리 연관관계 저장 (저장된 Entity 반환)
     * ✅ 성능 최적화: saveAll() 한 번만 호출하고 ID 포함한 Entity 반환
     */
    private List<ProductTag> saveProductTagsBatch(Product product, List<TagCategory> tagCategories) {
        if (tagCategories.isEmpty()) return Collections.emptyList();

        List<ProductTag> productTags = tagCategories.stream()
                .map(tagCategory -> ProductTag.of(product, tagCategory))
                .toList();

        // saveAll()이 ID가 할당된 Entity들을 반환 (추가 쿼리 없음)
        List<ProductTag> savedProductTags = productTagRepository.saveAll(productTags);

        log.info("Saved {} product tags for product ID: {}", savedProductTags.size(), product.getId());
        return savedProductTags;
    }

    /**
     * 응답 생성 최적화 (저장된 ProductTag 사용)
     * ✅ 성능 최적화: 추가 쿼리 없이 저장된 Entity 활용
     */
    private ProductResponse createProductResponseOptimized(
            Product savedProduct,
            List<Category> categories,
            List<ProductOption> savedOptions,
            ProductNotice savedNotice,
            List<ProductTag> savedProductTags) { // 저장된 ProductTag 사용

        List<CategoryResponse> categoryResponses = categories.stream()
                .map(CategoryResponse::from)
                .toList();

        List<ProductOptionResponse> optionResponses = savedOptions.stream()
                .map(ProductOptionResponse::from)
                .toList();

        ProductNoticeResponse noticeResponse = ProductNoticeResponse.from(savedNotice);

        // 저장된 ProductTag를 ProductTagResponse로 변환 (ID 포함)
        List<ProductTagResponse> productTagResponses = savedProductTags.stream()
                .map(ProductTagResponse::from)
                .toList();

        return ProductResponse.from(savedProduct, categoryResponses, optionResponses, noticeResponse, productTagResponses);
    }

// 기존 메서드들은 그대로 유지
    /**
     * 배치로 상품-카테고리 연관관계 저장
     */
    private void saveProductCategoriesBatch(Product product, List<Category> categories) {
        if (categories.isEmpty()) return;

        List<ProductCategory> productCategories = categories.stream()
                .map(category -> ProductCategory.of(product, category))
                .toList();

        productCategoryRepository.saveAll(productCategories);
    }

    /**
     * 배치로 상품 옵션 저장
     */
    private List<ProductOption> saveProductOptionsBatch(Product product, List<ProductOptionRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductOption> productOptions = requests.stream()
                .map(request -> request.toEntity(product))
                .toList();

        // saveAll 후 ID가 할당된 Entity 반환
        return productOptionRepository.saveAll(productOptions);
    }

    /**
     * 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Pageable pageable) {
        StopWatch stopWatch = new StopWatch("ProductListRetrieval");
        stopWatch.start("productQuery");

        log.info("Getting product list - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        // 1. 상품 목록 조회
        Page<Product> productPage = productRepository.findByStatusIn(
                Arrays.asList(Status.ACTIVE, Status.INACTIVE),
                pageable
        );
        stopWatch.stop();

        if (productPage.isEmpty()) {
            log.info("No products found");
            return Page.empty(pageable);
        }

        // 2. 카테고리 배치 조회 (N+1 해결)
        stopWatch.start("categoryBatchQuery");
        List<Product> products = productPage.getContent();
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<CategoryResponse>> categoriesMap = productCategoryRepository
                .findByProductIdsWithCategory(productIds).stream()
                .collect(Collectors.groupingBy(
                        pc -> pc.getProduct().getId(),               // 상품 ID로 그룹핑
                        Collectors.mapping(
                                pc -> CategoryResponse.from(pc.getCategory()), // DTO 변환
                                Collectors.toList()
                        )
                ));
        stopWatch.stop();

        // 3. 응답 생성
        stopWatch.start("responseCreation");
        List<ProductListResponse> content = products.stream()
                .map(product -> {
                    List<CategoryResponse> categories = categoriesMap
                            .getOrDefault(product.getId(), Collections.emptyList());
                    return ProductListResponse.from(product, categories);
                })
                .toList();

        Page<ProductListResponse> result = new PageImpl<>(content, pageable, productPage.getTotalElements());
        stopWatch.stop();

        log.info("Product list retrieved successfully - {} products | Performance: {}",
                content.size(), stopWatch.prettyPrint());

        return result;
    }

    /**
     * 상품 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        log.info("Getting product detail for product ID: {}", productId);

        // 모든 연관 데이터 조회
        Product product = productRepository.findByIdWithAllData(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다: " + productId));

        // 이미 FETCH JOIN으로 로딩된 데이터를 바로 사용
        List<CategoryResponse> categoryResponses = product.getProductCategories().stream()
                .map(ProductCategory::getCategory)     // Category 추출
                .map(CategoryResponse::from)           // DTO 변환
                .toList();

        List<ProductOptionResponse> optionResponses = product.getProductOptions().stream()
                .filter(option -> !option.getIsDeleted()) // 삭제되지 않은 옵션만
                .map(ProductOptionResponse::from)      // DTO 변환
                .toList();

        ProductNoticeResponse noticeResponse = ProductNoticeResponse.from(product.getProductNotice());

        ProductResponse response = ProductResponse.from(product, categoryResponses, optionResponses, noticeResponse);

        log.info("Product detail retrieved successfully for ID: {} (1 query)", productId);
        return response;
    }

}
