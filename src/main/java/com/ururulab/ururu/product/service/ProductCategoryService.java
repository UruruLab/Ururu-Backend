package com.ururulab.ururu.product.service;

import com.ururulab.ururu.product.controller.dto.response.CategoryResponse;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductCategory;
import com.ururulab.ururu.product.domain.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProductCategories(Product product, List<Category> categories) {
        if (categories.isEmpty()) return;

        List<ProductCategory> productCategories = categories.stream()
                .map(category -> ProductCategory.of(product, category))
                .toList();

        productCategoryRepository.saveAll(productCategories);
        log.info("Saved {} categories for product: {}", categories.size(), product.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<CategoryResponse> updateCategories(Product product, List<Category> newCategories) {
        // 기존 카테고리들 조회
        List<ProductCategory> existingProductCategories =
                productCategoryRepository.findByProductIdWithCategory(product.getId());

        Set<Long> existingCategoryIds = existingProductCategories.stream()
                .map(pc -> pc.getCategory().getId())
                .collect(Collectors.toSet());

        Set<Long> newCategoryIds = newCategories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        // 추가할 카테고리들 (새로운 것 - 기존 것)
        Set<Long> categoriesToAdd = new HashSet<>(newCategoryIds);
        categoriesToAdd.removeAll(existingCategoryIds);

        // 삭제할 카테고리들 (기존 것 - 새로운 것)
        Set<Long> categoriesToRemove = new HashSet<>(existingCategoryIds);
        categoriesToRemove.removeAll(newCategoryIds);

        // 변경사항이 있는 경우에만 처리
        if (!categoriesToAdd.isEmpty() || !categoriesToRemove.isEmpty()) {
            log.info("Categories changed for product: {}, add: {}, remove: {}",
                    product.getId(), categoriesToAdd, categoriesToRemove);

            // 삭제할 카테고리들 제거
            if (!categoriesToRemove.isEmpty()) {
                productCategoryRepository.deleteByProductIdAndCategoryIdIn(product.getId(), categoriesToRemove);
                log.info("Removed {} categories for product: {}", categoriesToRemove.size(), product.getId());
            }

            // 추가할 카테고리들 생성
            if (!categoriesToAdd.isEmpty()) {
                List<Category> categoriesToCreate = newCategories.stream()
                        .filter(category -> categoriesToAdd.contains(category.getId()))
                        .toList();
                saveProductCategories(product, categoriesToCreate);
                log.info("Added {} categories for product: {}", categoriesToAdd.size(), product.getId());
            }
        } else {
            log.info("Categories unchanged for product: {}", product.getId());
        }

        // 최종 카테고리 응답 반환
        return newCategories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CategoryResponse>> getProductCategoriesBatch(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return productCategoryRepository
                .findByProductIdsWithCategory(productIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pc -> pc.getProduct().getId(),
                        Collectors.mapping(
                                pc -> CategoryResponse.from(pc.getCategory()),
                                Collectors.toList()
                        )
                ));
    }
}
