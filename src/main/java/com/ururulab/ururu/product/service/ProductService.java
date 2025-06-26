package com.ururulab.ururu.product.service;

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
import com.ururulab.ururu.seller.domain.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

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
    //private final ProductOptionImageService productOptionImageService;


    /**
     * 상품을 등록합니다
     */
    @Transactional
    //public ProductResponse createProduct(ProductRequest productRequest, Seller seller) {
    public ProductResponse createProduct(ProductRequest productRequest) {
        StopWatch stopWatch = new StopWatch("ProductCreation");
        stopWatch.start("validation");

        log.info("Creating product: {}", productRequest.name());

        // 1. 요청 데이터 검증
        productValidator.validateProductRequest(productRequest);
        stopWatch.stop();

        // 2. 카테고리 유효성 검증 (Set 활용 최적화)
        stopWatch.start("categoryValidation");
        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        stopWatch.stop();

        // 3. 상품 저장
        stopWatch.start("productSave");
        //Product savedProduct = productRepository.save(productRequest.toEntity(seller));
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
     * 상품 목록 조회 - GET /products
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Pageable pageable) {
        StopWatch stopWatch = new StopWatch("ProductListRetrieval");
        stopWatch.start("productQuery");

        log.info("Getting product list - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<Product> productPage = productRepository.findByStatusIn(
                Arrays.asList(Status.ACTIVE, Status.INACTIVE),
                pageable
        );
        List<Product> products = productPage.getContent();

        stopWatch.stop();

        if (products.isEmpty()) {
            log.info("No products found");
            return Page.empty(pageable);
        }

        // 2. 카테고리 정보 배치 조회
        stopWatch.start("categoryBatchQuery");
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        List<ProductCategory> allProductCategories = productCategoryRepository.findByProductIdsWithCategory(productIds);

        Map<Long, List<ProductCategory>> categoriesMap = allProductCategories.stream()
                .collect(Collectors.groupingBy(pc -> pc.getProduct().getId()));

        stopWatch.stop();

        // 3. 응답 생성 및 Page 변환
        stopWatch.start("responseCreation");
        List<ProductListResponse> content = products.stream()
                .map(product -> {
                    List<CategoryResponse> categoryResponses = categoriesMap
                            .getOrDefault(product.getId(), Collections.emptyList())
                            .stream()
                            .map(pc -> CategoryResponse.from(pc.getCategory()))
                            .toList();

                    return ProductListResponse.from(product, categoryResponses);
                })
                .toList();

        Page<ProductListResponse> result = new PageImpl<>(content, pageable, productPage.getTotalElements());
        stopWatch.stop();

        log.info("Product list retrieved successfully - {} products | Performance: {}",
                content.size(), stopWatch.prettyPrint());

        return result;
    }
}
