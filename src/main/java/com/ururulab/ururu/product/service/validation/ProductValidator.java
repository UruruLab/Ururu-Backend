package com.ururulab.ururu.product.service.validation;

import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationMessages.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidator {

    private final CategoryRepository categoryRepository;

    /**
     * 상품 등록 요청 데이터를 검증합니다
     */
    public void validateProductRequest(ProductRequest productRequest) {
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

    /**
     * 카테고리 유효성 검증
     */
    public List<Category> validateAndGetCategoriesOptimized(List<Long> categoryIds) {
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
}
