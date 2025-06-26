package com.ururulab.ururu.product.service.validation;

import com.ururulab.ururu.global.common.entity.TagCategory;
import com.ururulab.ururu.global.common.repository.TagCategoryRepository;
import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationMessages.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidator {

    private final CategoryRepository categoryRepository;
    private final TagCategoryRepository tagCategoryRepository;

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

        // 1. 중복 제거를 List로 바로 처리
        List<Long> uniqueCategoryIds = categoryIds.stream().distinct().toList();

        // 2. 배치 조회
        List<Category> categories = categoryRepository.findAllById(uniqueCategoryIds);

        // 3. 존재 여부 확인 최적화
        if (categories.size() != uniqueCategoryIds.size()) {
            Map<Long, Category> categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, category -> category));

            List<Long> missingIds = uniqueCategoryIds.stream()
                    .filter(id -> !categoryMap.containsKey(id))
                    .toList();

            throw new IllegalArgumentException("존재하지 않는 카테고리: " + missingIds);
        }

        return categories;
    }

    /**
     * 태그 카테고리 유효성 검증 후 TagCategory 엔티티 반환
     */
    public List<TagCategory> validateAndGetTagCategories(List<Long> tagCategoryIds) {
        if (tagCategoryIds == null || tagCategoryIds.isEmpty()) {
            throw new IllegalArgumentException(TAG_CATEGORIES_REQUIRED);
        }

        // 1. 중복 제거
        List<Long> uniqueIds = tagCategoryIds.stream().distinct().toList();
        if (uniqueIds.size() != tagCategoryIds.size()) {
            throw new IllegalArgumentException(TAG_CATEGORIES_DUPLICATE);
        }

        // 2. 배치 조회
        List<TagCategory> tagCategories = tagCategoryRepository.findAllById(uniqueIds);

        // 3. 존재 여부 확인
        if (tagCategories.size() != uniqueIds.size()) {
            throw new IllegalArgumentException(TAG_CATEGORIES_NOT_EXIST);
        }

        // 4. 활성화 상태 확인 - 스트림 최적화
        List<TagCategory> activeCategories = tagCategories.stream()
                .filter(TagCategory::getIsActive)
                .toList();

        if (activeCategories.size() != uniqueIds.size()) {
            // 비활성화된 카테고리 ID 찾기
            Set<Long> activeIds = activeCategories.stream()
                    .map(TagCategory::getId)
                    .collect(Collectors.toSet());

            List<Long> inactiveIds = uniqueIds.stream()
                    .filter(id -> !activeIds.contains(id))
                    .toList();

            throw new IllegalArgumentException("비활성화된 태그 카테고리: " + inactiveIds);
        }

        return activeCategories;
    }
}
