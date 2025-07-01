package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryManagementService {
    private final CategoryRepository categoryRepository;
    private final TagCategoryRepository tagCategoryRepository;

    // 카테고리 수정
    @CacheEvict(value = "category", key = "#categoryId")
    public Category updateCategory(Long categoryId, String newName) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(CATEGORIES_NOT_EXIST));
        category.updateName(newName);
        return categoryRepository.save(category);
    }

    // 카테고리 삭제
    @CacheEvict(value = "category", key = "#categoryId")
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    // 태그 카테고리 수정
    @CacheEvict(value = "tagCategory", key = "#tagCategoryId")
    public TagCategory updateTagCategory(Long tagCategoryId, String newName) {
        TagCategory tagCategory = tagCategoryRepository.findById(tagCategoryId)
                .orElseThrow(() -> new EntityNotFoundException(TAG_CATEGORIES_NOT_EXIST));
        tagCategory.updateName(newName);
        return tagCategoryRepository.save(tagCategory);
    }

    // 태그 카테고리 삭제
    @CacheEvict(value = "tagCategory", key = "#tagCategoryId")
    public void deleteTagCategory(Long tagCategoryId) {
        tagCategoryRepository.deleteById(tagCategoryId);
    }

    // 전체 캐시 초기화
    @CacheEvict(value = {"category", "tagCategory"}, allEntries = true)
    public void clearAllCache() {
        // 캐시만 초기화
    }
}
