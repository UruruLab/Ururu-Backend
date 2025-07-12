package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import com.ururulab.ururu.product.dto.common.CategoryTreeDto;
import com.ururulab.ururu.product.dto.response.ProductMetadataResponse;
import com.ururulab.ururu.product.dto.common.TagDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class ProductMetadataService {

    private final CategoryRepository categoryRepository;
    private final TagCategoryRepository tagCategoryRepository;

    public ProductMetadataResponse getMetadata() {
        List<Category> categories = categoryRepository.findAll();
        List<TagCategory> tags = tagCategoryRepository.findAllByIsActiveTrueOrderByDisplayOrder();

        if (categories == null || categories.isEmpty()) {
            throw new BusinessException(CATEGORY_NOT_FOUND); // 직접 정의한 에러코드
        }

        if (tags == null || tags.isEmpty()) {
            throw new BusinessException(TAG_NOT_FOUND); // 직접 정의한 에러코드
        }

        List<CategoryTreeDto> categoryTree = buildCategoryTree(categories);
        List<TagDto> tagDtos = tags.stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName()))
                .toList();

        return new ProductMetadataResponse(categoryTree, tagDtos);
    }

    private List<CategoryTreeDto> buildCategoryTree(List<Category> categories) {
        Map<Long, List<Category>> grouped = categories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        return categories.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> toTreeDto(c, grouped))
                .toList();
    }

    private CategoryTreeDto toTreeDto(Category category, Map<Long, List<Category>> group) {
        List<CategoryTreeDto> children = Optional.ofNullable(group.get(category.getId()))
                .orElse(List.of())
                .stream()
                .map(child -> toTreeDto(child, group))
                .toList();

        return new CategoryTreeDto(category.getId(), category.getName(), children);
    }
}
