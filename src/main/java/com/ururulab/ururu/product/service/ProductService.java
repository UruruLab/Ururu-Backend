package com.ururulab.ururu.product.service;

import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.CategoryResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductNoticeResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductResponse;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductCategory;
import com.ururulab.ururu.product.domain.entity.ProductNotice;
import com.ururulab.ururu.product.domain.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductNoticeRepository productNoticeRepository;
    private final ProductOptionRepository productOptionRepository;

    /**
     * Product 상품을 저장합니다
     *
     * @param productRequest
     * @return
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        validateProductRequest(productRequest);

        Product savedProduct = productRepository.save(productRequest.toEntity());

        // 2. 카테고리 유효성 검증 및 조회
        List<Category> categories = validateAndGetCategories(productRequest.categoryIds());

        List<CategoryResponse> categoryResponses = saveProductCategories(savedProduct, categories);

        // 상품 정보고시 저장
        ProductNoticeResponse productNoticeResponse = saveProductNotice(savedProduct, productRequest);

        return ProductResponse.from(savedProduct, categoryResponses, productOptionResponses, productNoticeResponse);
    }

    /**
     * 상품 정보고시를 저장합니다.
     *
     * @param product 저장된 상품 엔티티
     * @param productRequest 상품 등록 요청 정보
     * @return 저장된 상품 정보고시 응답
     */
    private ProductNoticeResponse saveProductNotice(Product product, ProductRequest productRequest) {
        ProductNotice productNotice = productRequest.productNotice().toEntity(product);
        ProductNotice savedProductNotice = productNoticeRepository.save(productNotice);

        return ProductNoticeResponse.from(savedProductNotice);
    }

    /**
     * 상품과 카테고리 간의 연관관계를 저장합니다.
     *
     * @param product 저장된 상품 엔티티
     * @param categories 연관시킬 카테고리 목록
     * @return 저장된 카테고리 응답 목록
     */
    private List<CategoryResponse> saveProductCategories(Product product, List<Category> categories){
        List<ProductCategory> productCategories = categories.stream()
                .map(category -> ProductCategory.of(product, category))
                .toList();

        productCategoryRepository.saveAll(productCategories);

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * 카테고리 유효성을 검증하고 Category 엔티티들을 반환합니다.
     *
     * @param categoryIds 카테고리 ID 목록
     * @return 검증된 Category 엔티티 목록
     * @throws IllegalArgumentException 존재하지 않는 카테고리가 포함된 경우
     */
    private List<Category> validateAndGetCategories(List<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        if (categories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("유효하지 않은 카테고리가 포함되어 있습니다.");
        }

        return categories;
    }

    /**
     * 상품 등록 요청 데이터를 검증합니다.
     *
     * @param productRequest 검증할 상품 요청 데이터
     * @throws IllegalArgumentException 유효하지 않은 데이터가 있는 경우
     */
    private void validateProductRequest(ProductRequest productRequest) {
        if (productRequest.categoryIds() == null || productRequest.categoryIds().isEmpty()) {
            throw new IllegalArgumentException("카테고리는 최소 1개 이상 선택해야 합니다.");
        }

        if (productRequest.productOptions() == null || productRequest.productOptions().isEmpty()) {
            throw new IllegalArgumentException("상품 옵션은 최소 1개 이상 등록해야 합니다.");
        }

        if (productRequest.productNotice() == null) {
            throw new IllegalArgumentException("상품 정보고시는 필수 입력 항목입니다.");
        }
    }
}
