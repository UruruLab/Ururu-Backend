package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
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
    public Category findCategoryById(Long categoryId) {
        log.info("DB hit: categoryId = {}", categoryId);
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(CATEGORY_NOT_FOUND));
    }

    @Cacheable(value = "tagCategory", key = "#tagCategoryId")
    public TagCategory findTagCategoryById(Long tagCategoryId) {
        log.info("DB hit: tagCategoryId = {}", tagCategoryId);
        return tagCategoryRepository.findById(tagCategoryId)
                .orElseThrow(() -> new BusinessException(TAG_NOT_FOUND));
    }
}
