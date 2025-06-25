package com.ururulab.ururu.product.service;

import com.ururulab.ururu.product.domain.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.CategoryResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductNoticeResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductOptionResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductResponse;
import com.ururulab.ururu.product.domain.entity.*;
import com.ururulab.ururu.product.domain.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationMessages.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductNoticeRepository productNoticeRepository;
    private final ProductOptionRepository productOptionRepository;
    //private final ProductOptionImageService productOptionImageService;


    /**
     * 상품을 등록합니다
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        StopWatch stopWatch = new StopWatch("ProductCreation");
        stopWatch.start("validation");

        log.info("Creating product: {}", productRequest.name());

        // 1. 요청 데이터 검증
        validateProductRequest(productRequest);
        stopWatch.stop();

        // 2. 카테고리 유효성 검증 (Set 활용 최적화)
        stopWatch.start("categoryValidation");
        List<Category> categories = validateAndGetCategoriesOptimized(productRequest.categoryIds());
        stopWatch.stop();

        // 3. 상품 저장
        stopWatch.start("productSave");
        Product savedProduct = productRepository.save(productRequest.toEntity());
        stopWatch.stop();

        // 4. 배치로 연관 데이터 저장 (재조회 제거)
        stopWatch.start("relatedDataSave");
        saveProductCategoriesBatch(savedProduct, categories);
        List<ProductOption> savedOptions = saveProductOptionsBatch(savedProduct, productRequest.productOptions());
        ProductNotice savedNotice = productNoticeRepository.save(productRequest.productNotice().toEntity(savedProduct));
        stopWatch.stop();

        // 5. 응답 생성 (insert 후 Entity 활용, 추가 쿼리 제거)
        stopWatch.start("responseCreation");
        ProductResponse response = createProductResponseOptimized(savedProduct, categories, savedOptions, savedNotice);
        stopWatch.stop();

        log.info("Product created successfully with ID: {} | Performance: {}",
                savedProduct.getId(), stopWatch.prettyPrint());

        return response;
    }

    /**
     * 카테고리 유효성 검증
     */
    private List<Category> validateAndGetCategoriesOptimized(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException(CATEGORIES_REQUIRED);
        }

        // Set으로 중복 제거 후 조회
        Set<Long> uniqueCategoryIds = new LinkedHashSet<>(categoryIds);
        List<Category> categories = categoryRepository.findAllById(uniqueCategoryIds);

        // 존재 여부 확인
        Set<Long> foundCategoryIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = uniqueCategoryIds.stream()
                .filter(id -> !foundCategoryIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리: " + missingIds);
        }

        return categories;
    }

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
     * 응답 생성 최적화
     */
    private ProductResponse createProductResponseOptimized(
            Product savedProduct,
            List<Category> categories,
            List<ProductOption> savedOptions,  // insert 후 Entity 활용
            ProductNotice savedNotice) {

        List<CategoryResponse> categoryResponses = categories.stream()
                .map(CategoryResponse::from)
                .toList();

        // insert 후 반환된 Entity 활용
        List<ProductOptionResponse> optionResponses = savedOptions.stream()
                .map(ProductOptionResponse::from)
                .toList();

        ProductNoticeResponse noticeResponse = ProductNoticeResponse.from(savedNotice);

        return ProductResponse.from(savedProduct, categoryResponses, optionResponses, noticeResponse);
    }

    /**
     * 상품 등록 요청 데이터를 검증합니다
     */
    private void validateProductRequest(ProductRequest productRequest) {
        if (productRequest.categoryIds() == null || productRequest.categoryIds().isEmpty()) {
            throw new IllegalArgumentException(CATEGORIES_REQUIRED);
        }

        if (productRequest.productOptions() == null || productRequest.productOptions().isEmpty()) {
            throw new IllegalArgumentException(PRODUCT_OPTIONS_REQUIRED);
        }

        if (productRequest.productNotice() == null) {
            throw new IllegalArgumentException(PRODUCT_NOTICE_REQUIRED);
        }
    }
}
