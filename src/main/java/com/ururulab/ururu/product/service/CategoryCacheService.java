package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import com.ururulab.ururu.product.dto.common.CategoryCacheDto;
import com.ururulab.ururu.product.dto.common.TagCategoryCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryCacheService {
    private final CategoryRepository categoryRepository;
    private final TagCategoryRepository tagCategoryRepository;

    @Cacheable(value = "category", key = "#categoryId")
    public CategoryCacheDto findCategoryDto(Long categoryId) {
        log.info("캐시 미스 발생 → DB에서 카테고리 조회. categoryId = {}", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(CATEGORY_NOT_FOUND));
        return CategoryCacheDto.from(category);
    }

    @Cacheable(value = "tagCategory", key = "#tagCategoryId")
    public TagCategoryCacheDto findTagCategoryDto(Long tagCategoryId) {
        log.info("캐시 미스 발생 → DB에서 태그 카테고리 조회. tagCategoryId = {}", tagCategoryId);
        TagCategory tagCategory = tagCategoryRepository.findById(tagCategoryId)
                .orElseThrow(() -> new BusinessException(TAG_NOT_FOUND));
        return TagCategoryCacheDto.from(tagCategory);
    }
}
