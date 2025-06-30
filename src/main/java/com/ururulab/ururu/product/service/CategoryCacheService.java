package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리를 찾을 수 없습니다. ID=" + categoryId));
    }

    @Cacheable(value = "tagCategory", key = "#tagCategoryId")
    public TagCategory findTagCategoryById(Long tagCategoryId) {
        log.info("DB hit: tagCategoryId = {}", tagCategoryId);
        return tagCategoryRepository.findById(tagCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 태그 카테고리를 찾을 수 없습니다. ID=" + tagCategoryId));
    }
}
