package com.ururulab.ururu.product.service.validation;

import com.ururulab.ururu.global.domain.entity.TagCategory;
import com.ururulab.ururu.global.domain.repository.TagCategoryRepository;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;
import com.ururulab.ururu.product.domain.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.domain.entity.Category;
import com.ururulab.ururu.product.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationMessages.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidator {

    private final CategoryRepository categoryRepository;
    private final TagCategoryRepository tagCategoryRepository;

    /**
     * 카테고리 유효성 검증
     */
    @Cacheable(value = "categories", key = "#categoryIds")
    public List<Category> validateAndGetCategoriesOptimized(List<Long> categoryIds) {
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

    public void validateOptionImagePair(List<ProductOptionRequest> options, List<MultipartFile> images) {
        validateOptionImageCount(options, images);
        validateAllImages(images);
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

    /**
     * 상품 옵션 이미지 검증
     */
    public void validateImage(MultipartFile image) {
        long start = System.currentTimeMillis();

        if (image == null || image.isEmpty()) {
            return;
        }

        ImageFormat extFmt = parseExtension(image);
        ImageFormat mimeFmt = parseMimeType(image);
        ensureMatchingFormats(extFmt, mimeFmt, image);

        long end = System.currentTimeMillis();
        log.info("Image validation took: {}ms for file: {}", end - start, image.getOriginalFilename());
    }

    public void validateAllImages(List<MultipartFile> optionImages) {
        if (optionImages == null || optionImages.isEmpty()) {
            return;
        }

        for (int i = 0; i < optionImages.size(); i++) {
            MultipartFile imageFile = optionImages.get(i);

            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }

            try {
                validateImage(imageFile);  // 기존 메서드 재사용
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("옵션 %d번째 이미지가 유효하지 않습니다: %s", i + 1, e.getMessage()), e
                );
            }
        }
    }

    private ImageFormat parseExtension(MultipartFile file) {
        String filename = Optional.ofNullable(file.getOriginalFilename())
                .filter(n -> n.contains("."))
                .orElseThrow(() ->
                        new InvalidImageFormatException("파일명이 없거나 확장자를 찾을 수 없습니다.")
                );
        int idx = filename.lastIndexOf('.');
        if (idx == filename.length() - 1) {
            throw new InvalidImageFormatException("파일명이 마침표로 끝납니다: " + filename);
        }
        String ext = filename.substring(idx + 1).toLowerCase();
        return ImageFormat.fromExtension(ext)
                .orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 확장자: " + ext));
    }

    private ImageFormat parseMimeType(MultipartFile file) {
        String mime = Optional.ofNullable(file.getContentType())
                .orElseThrow(() ->
                        new InvalidImageFormatException("MIME 타입을 확인할 수 없습니다.")
                );
        return ImageFormat.fromMimeType(mime)
                .orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 MIME 타입: " + mime));
    }

    private void ensureMatchingFormats(
            ImageFormat extFmt,
            ImageFormat mimeFmt,
            MultipartFile file
    ) {
        if (extFmt != mimeFmt) {
            throw new InvalidImageFormatException(
                    String.format(
                            "확장자(%s)와 MIME(%s)이 일치하지 않습니다: file=%s",
                            extFmt.getExtension(),
                            mimeFmt.getMimeType(),
                            file.getOriginalFilename()
                    )
            );
        }
    }
}
