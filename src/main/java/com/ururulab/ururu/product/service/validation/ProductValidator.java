package com.ururulab.ururu.product.service.validation;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.image.validation.ImageValidator;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import com.ururulab.ururu.product.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.domain.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidator {

    private final ImageValidator imageValidator;
    private final CategoryRepository categoryRepository;
    private final TagCategoryRepository tagCategoryRepository;

    /**
     * 카테고리 유효성 검증
     */
    public List<Category> validateAndGetCategoriesOptimized(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }

        log.info("카테고리 검증 시작. categoryIds = {}", categoryIds);

        // 1. 중복 제거 및 정렬
        List<Long> uniqueSortedIds = categoryIds.stream()
                .distinct()
                .sorted()
                .toList();

        // 2. 배치 조회 (N+1 해결)
        List<Category> categories = categoryRepository.findAllById(uniqueSortedIds);

        // 3. 존재하지 않는 카테고리 검증
        if (categories.size() != uniqueSortedIds.size()) {
            List<Long> foundIds = categories.stream()
                    .map(Category::getId)
                    .toList();
            List<Long> notFoundIds = uniqueSortedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            log.warn("존재하지 않는 카테고리 ID: {}", notFoundIds);
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND,
                    "존재하지 않는 카테고리: " + notFoundIds);
        }

        // 4. 원본 순서 유지하여 반환
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Category> result = categoryIds.stream()
                .map(categoryMap::get)
                .toList();

        log.info("카테고리 검증 완료. 검증된 카테고리 수: {}", result.size());
        return result;
    }

    public List<TagCategory> validateAndGetTagCategories(List<Long> tagCategoryIds) {
        if (tagCategoryIds == null || tagCategoryIds.isEmpty()) {
            return List.of();
        }

        log.info("태그카테고리 검증 시작. tagCategoryIds = {}", tagCategoryIds);

        // 1. 중복 제거 및 정렬
        List<Long> uniqueSortedIds = tagCategoryIds.stream()
                .distinct()
                .sorted()
                .toList();

        // 2. 배치 조회 (N+1 해결)
        List<TagCategory> tagCategories = tagCategoryRepository.findAllById(uniqueSortedIds);

        // 3. 존재하지 않는 태그카테고리 검증
        if (tagCategories.size() != uniqueSortedIds.size()) {
            List<Long> foundIds = tagCategories.stream()
                    .map(TagCategory::getId)
                    .toList();
            List<Long> notFoundIds = uniqueSortedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            log.warn("존재하지 않는 태그카테고리 ID: {}", notFoundIds);
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND,
                    "존재하지 않는 태그카테고리: " + notFoundIds);
        }

        // 4. 원본 순서 유지하여 반환
        Map<Long, TagCategory> tagCategoryMap = tagCategories.stream()
                .collect(Collectors.toMap(TagCategory::getId, Function.identity()));

        List<TagCategory> result = tagCategoryIds.stream()
                .map(tagCategoryMap::get)
                .toList();

        log.info("태그카테고리 검증 완료. 검증된 태그카테고리 수: {}", result.size());
        return result;
    }


    public void validateOptionImagePair(List<ProductOptionRequest> options, List<MultipartFile> images) {
        validateOptionImageCount(options, images);
        imageValidator.validateAllImages(images);
    }

    /**
     * 상품 옵션과 이미지 개수 일치 검증
     */
    public void validateOptionImageCount(List<ProductOptionRequest> options,
                                         List<MultipartFile> images) {
        if (options.size() != images.size()) {
            throw new BusinessException(ErrorCode.OPTION_IMAGE_COUNT_MISMATCH,
                    options.size(), images.size());
        }
    }
}
