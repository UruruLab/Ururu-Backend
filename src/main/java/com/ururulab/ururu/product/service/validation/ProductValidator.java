package com.ururulab.ururu.product.service.validation;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.image.validation.ImageValidator;
import com.ururulab.ururu.product.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.service.CategoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidator {

    private final CategoryCacheService categoryCacheService;
    private final ImageValidator imageValidator;

    /**
     * 카테고리 유효성 검증
     */
    public List<Category> validateAndGetCategoriesOptimized(List<Long> categoryIds) {
        return categoryIds.stream()
                .distinct()
                .sorted()
                .map(categoryCacheService::findCategoryById) // 캐시 + 존재 검증 포함
                .toList();
    }

    public List<TagCategory> validateAndGetTagCategories(List<Long> tagCategoryIds) {
        return tagCategoryIds.stream()
                .distinct()
                .sorted()
                .map(categoryCacheService::findTagCategoryById)
                .toList();
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
