package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.product.dto.response.ProductTagResponse;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductTag;
import com.ururulab.ururu.product.domain.repository.ProductTagRepository;
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
public class ProductTagService {

    private final ProductTagRepository productTagRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public List<ProductTag> saveProductTags(Product product, List<TagCategory> tagCategories) {
        if (tagCategories.isEmpty()) return Collections.emptyList();

        List<ProductTag> productTags = tagCategories.stream()
                .map(tagCategory -> ProductTag.of(product, tagCategory))
                .toList();

        return productTagRepository.saveAll(productTags);
    }

    /**
     * 태그 업데이트 - 변경된 것만 추가/삭제
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<ProductTagResponse> updateTags(Product product, List<TagCategory> newTagCategories) {
        // 기존 태그들 조회
        List<ProductTag> existingProductTags = productTagRepository.findByProductIdWithTagCategory(product.getId());
        Set<Long> existingTagIds = existingProductTags.stream()
                .map(pt -> pt.getTagCategory().getId())
                .collect(Collectors.toSet());

        Set<Long> newTagIds = newTagCategories.stream()
                .map(TagCategory::getId)
                .collect(Collectors.toSet());

        // 추가할 태그들
        Set<Long> tagsToAdd = new HashSet<>(newTagIds);
        tagsToAdd.removeAll(existingTagIds);

        // 삭제할 태그들
        Set<Long> tagsToRemove = new HashSet<>(existingTagIds);
        tagsToRemove.removeAll(newTagIds);

        // 변경사항이 있는 경우에만 처리
        if (!tagsToAdd.isEmpty() || !tagsToRemove.isEmpty()) {
            log.info("Tags changed for product: {}, add: {}, remove: {}",
                    product.getId(), tagsToAdd, tagsToRemove);

            // 삭제할 태그들 제거
            if (!tagsToRemove.isEmpty()) {
                productTagRepository.deleteByProductIdAndTagCategoryIdIn(product.getId(), tagsToRemove);
                log.info("Removed {} tags for product: {}", tagsToRemove.size(), product.getId());
            }

            // 추가할 태그들 생성
            if (!tagsToAdd.isEmpty()) {
                List<TagCategory> tagCategoriesToCreate = newTagCategories.stream()
                        .filter(tagCategory -> tagsToAdd.contains(tagCategory.getId()))
                        .toList();
                List<ProductTag> savedTags = saveProductTags(product, tagCategoriesToCreate);
                log.info("Added {} tags for product: {}", tagsToAdd.size(), product.getId());
            }
        } else {
            log.info("Tags unchanged for product: {}", product.getId());
        }

        // 최종 태그 응답 반환
        return newTagCategories.stream()
                .map(tagCategory -> ProductTagResponse.from(
                        ProductTag.of(product, tagCategory) // 응답용 임시 객체
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ProductTagResponse>> getProductTagsBatch(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return productTagRepository
                .findByProductIdsWithTagCategory(productIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pt -> pt.getProduct().getId(),
                        Collectors.mapping(
                                ProductTagResponse::from,
                                Collectors.toList()
                        )
                ));
    }
}
