package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.product.domain.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.domain.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.*;
import com.ururulab.ururu.product.domain.entity.*;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.*;
import com.ururulab.ururu.product.event.ProductImageUploadEvent;
import com.ururulab.ururu.product.service.validation.ProductValidator;
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

import java.io.IOException;
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
    private final ProductTagRepository productTagRepository;
    private final ProductValidator productValidator;
    private final ProductOptionImageService productOptionImageService;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 상품 등록 - 비동기 이미지 업로드
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> optionImages) {
        StopWatch stopWatch = new StopWatch("ProductCreation");
        stopWatch.start("validation");

        log.info("Creating product: {}", productRequest.name());

        // 1. 요청 데이터 검증 (이미지 검증 포함)
        productValidator.validateProductRequest(productRequest);

        if (optionImages != null && !optionImages.isEmpty()) {
            validateImageCount(productRequest.productOptions(), optionImages);
            validateAllImages(optionImages); // 업로드 전 검증
        }
        stopWatch.stop();

        // 2. 카테고리 및 태그카테고리 유효성 검증
        stopWatch.start("categoryValidation");
        List<Category> categories = productValidator.validateAndGetCategoriesOptimized(productRequest.categoryIds());
        List<TagCategory> tagCategories = productValidator.validateAndGetTagCategories(productRequest.tagCategoryIds());
        stopWatch.stop();

        // 3. 상품 저장
        stopWatch.start("productSave");
        Product savedProduct = productRepository.save(productRequest.toEntity());
        stopWatch.stop();

        // 4. 배치로 연관 데이터 저장
        stopWatch.start("relatedDataSave");
        saveProductCategoriesBatch(savedProduct, categories);
        List<ProductTag> savedProductTags = saveProductTagsBatch(savedProduct, tagCategories);
        List<ProductOption> savedOptions = saveProductOptionsBatch(savedProduct, productRequest.productOptions());
        ProductNotice savedNotice = productNoticeRepository.save(productRequest.productNotice().toEntity(savedProduct));
        stopWatch.stop();

        // 5. 비동기 이미지 업로드 이벤트 발행 (기존 동기식 업로드 제거)
        if (optionImages != null && !optionImages.isEmpty()) {
            stopWatch.start("eventPublish");
            List<ProductImageUploadRequest> uploadRequests = createImageUploadRequests(savedOptions, optionImages);
            eventPublisher.publishEvent(new ProductImageUploadEvent(savedProduct.getId(), uploadRequests));
            stopWatch.stop();
        }

        // 6. 응답 생성 (이미지 URL은 아직 null)
        stopWatch.start("responseCreation");
        ProductResponse response = createProductResponseOptimized(savedProduct, categories, savedOptions, savedNotice, savedProductTags);
        stopWatch.stop();

        log.info("Product created successfully with ID: {} | Performance: {}",
                savedProduct.getId(), stopWatch.prettyPrint());

        return response;
    }

    /**
     * 이미지 업로드 요청 생성
     */
    private List<ProductImageUploadRequest> createImageUploadRequests(
            List<ProductOption> savedOptions, List<MultipartFile> optionImages) {

        List<ProductImageUploadRequest> requests = new ArrayList<>();

        for (int i = 0; i < savedOptions.size() && i < optionImages.size(); i++) {
            MultipartFile imageFile = optionImages.get(i);

            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }

            ProductOption option = savedOptions.get(i);

            try {
                requests.add(new ProductImageUploadRequest(
                        option.getId(),
                        imageFile.getOriginalFilename(),
                        imageFile.getBytes()
                ));
            } catch (IOException e) {
                log.error("Failed to read image file for option: {}", option.getId(), e);
                throw new RuntimeException("이미지 파일 읽기 실패" + e.getMessage(), e);
            }
        }

        return requests;
    }

    // 기존 메서드들 유지
    private void validateImageCount(List<ProductOptionRequest> options, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        if (images.size() != options.size()) {
            throw new IllegalArgumentException(
                    String.format("옵션 개수(%d)와 이미지 개수(%d)가 일치하지 않습니다.", options.size(), images.size())
            );
        }
    }

    private void validateAllImages(List<MultipartFile> optionImages) {
        if (optionImages == null || optionImages.isEmpty()) {
            return;
        }

        for (int i = 0; i < optionImages.size(); i++) {
            MultipartFile imageFile = optionImages.get(i);

            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }

            try {
                productOptionImageService.validateImage(imageFile);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("옵션 %d번째 이미지가 유효하지 않습니다: %s", i + 1, e.getMessage()), e
                );
            }
        }
    }

    private void saveProductCategoriesBatch(Product product, List<Category> categories) {
        if (categories.isEmpty()) return;

        List<ProductCategory> productCategories = categories.stream()
                .map(category -> ProductCategory.of(product, category))
                .toList();

        productCategoryRepository.saveAll(productCategories);
    }

    private List<ProductTag> saveProductTagsBatch(Product product, List<TagCategory> tagCategories) {
        if (tagCategories.isEmpty()) return Collections.emptyList();

        List<ProductTag> productTags = tagCategories.stream()
                .map(tagCategory -> ProductTag.of(product, tagCategory))
                .toList();

        return productTagRepository.saveAll(productTags);
    }

    private List<ProductOption> saveProductOptionsBatch(Product product, List<ProductOptionRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductOption> productOptions = requests.stream()
                .map(request -> request.toEntity(product))
                .toList();

        return productOptionRepository.saveAll(productOptions);
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

        // 2. 카테고리 및 태그 배치 조회
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

        stopWatch.start("tagBatchQuery");
        Map<Long, List<ProductTagResponse>> tagsMap = productTagRepository
                .findByProductIdsWithTagCategory(productIds).stream()
                .collect(Collectors.groupingBy(
                        pt -> pt.getProduct().getId(),              // 상품 ID로 그룹핑
                        Collectors.mapping(
                                ProductTagResponse::from,            // DTO 변환
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

                    List<ProductTagResponse> tags = tagsMap
                            .getOrDefault(product.getId(), Collections.emptyList());

                    return ProductListResponse.from(product, categories, tags);
                })
                .toList();

        Page<ProductListResponse> result = new PageImpl<>(content, pageable, productPage.getTotalElements());
        stopWatch.stop();

        log.info("Product list retrieved successfully - {} products | Performance: {}",
                content.size(), stopWatch.prettyPrint());

        return result;
    }

}
