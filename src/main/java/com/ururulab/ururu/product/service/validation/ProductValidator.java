package com.ururulab.ururu.product.service.validation;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
    @Cacheable(value = "categories", key = "#categoryIds")
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

            throw new IllegalArgumentException(CATEGORIES_NOT_EXIST + ": " + missingIds);
        }

        return categories;
    }

    /**
     * 태그 카테고리 유효성 검증 후 TagCategory 엔티티 반환
     */
    @Cacheable(value = "tagCategories", key = "#tagCategoryIds")
    public List<TagCategory> validateAndGetTagCategories(List<Long> tagCategoryIds) {
        if (tagCategoryIds == null || tagCategoryIds.isEmpty()) {
            throw new IllegalArgumentException(TAG_CATEGORIES_REQUIRED);
        }

        // 1. 중복 제거
        List<Long> uniqueIds = tagCategoryIds.stream().distinct().toList();

        // 2. 배치 조회
        List<TagCategory> tagCategories = tagCategoryRepository.findAllById(uniqueIds);

        // 3. 존재 여부 확인
        if (tagCategories.size() != uniqueIds.size()) {
            Map<Long, TagCategory> foundMap = tagCategories.stream()
                    .collect(Collectors.toMap(TagCategory::getId, t -> t));

            List<Long> missingIds = uniqueIds.stream()
                    .filter(id -> !foundMap.containsKey(id))
                    .toList();

            throw new IllegalArgumentException(TAG_CATEGORIES_NOT_EXIST + ": "+ missingIds);
        }

        return tagCategories;
    }
}
