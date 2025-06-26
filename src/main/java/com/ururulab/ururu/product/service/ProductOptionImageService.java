package com.ururulab.ururu.product.service;

import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static com.ururulab.ururu.image.domain.ImageCategory.PRODUCTS;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionImageService {
    private final ImageService imageService;
    private final ProductOptionRepository productOptionRepository;

    /**
     * 상품 옵션 이미지 검증
     */
    public void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }
        validateSingleImage(image);
    }

    private void validateSingleImage(MultipartFile file) {
        ImageFormat extFmt = parseExtension(file);
        ImageFormat mimeFmt = parseMimeType(file);
        ensureMatchingFormats(extFmt, mimeFmt, file);
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

    /**
     * 상품 옵션 이미지 단일 업로드 (MultipartFile 버전)
     */
    public String uploadProductOptionImage(MultipartFile file) {
        try {
            validateImage(file);
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new IllegalArgumentException("파일명이 없습니다."));

            String imageUrl = imageService.uploadImage(
                    PRODUCTS.getPath(),
                    filename,
                    file.getBytes()
            );
            log.info("Product option image uploaded: {}", imageUrl);
            return imageUrl;
        } catch (IOException e) {
            log.error("IO Error while uploading product option image: {}", e.getMessage());
            throw new RuntimeException("상품 옵션 이미지 업로드 중 IO 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 옵션 이미지 업로드 (byte[] 버전 - 이벤트 처리용)
     */
    public String uploadImageFromBytes(String originalFilename, byte[] imageData) {
        try {
            String imageUrl = imageService.uploadImage(
                    PRODUCTS.getPath(),
                    originalFilename,
                    imageData
            );
            log.info("Product option image uploaded from bytes: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("Error while uploading product option image from bytes: {}", e.getMessage());
            throw new RuntimeException("상품 옵션 이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 옵션의 기존 이미지를 새 이미지로 교체하고 DB 업데이트
     */
    @Transactional
    public String updateProductOptionImage(Long productOptionId, MultipartFile newImageFile) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 옵션입니다: " + productOptionId));

        // 새 이미지 업로드 (기존 이미지는 S3에서 자동으로 관리됨)
        String newImageUrl = uploadProductOptionImage(newImageFile);

        // DB에 새 이미지 URL 업데이트
        productOption.updateImageUrl(newImageUrl);
        productOptionRepository.save(productOption);

        log.info("Product option image updated for option ID: {}", productOptionId);
        return newImageUrl;
    }

    /**
     * 상품 옵션 이미지 URL만 업데이트 (이벤트 처리용)
     */
    @Transactional
    public void updateProductOptionImageUrl(Long productOptionId, String imageUrl) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 옵션입니다: " + productOptionId));

        productOption.updateImageUrl(imageUrl);
        productOptionRepository.save(productOption);

        log.info("Product option image URL updated for option ID: {} -> {}", productOptionId, imageUrl);
    }

    /**
     * 상품 옵션 이미지 삭제하고 DB에서 URL 제거
     */
    @Transactional
    public void deleteProductOptionImageByOptionId(Long productOptionId) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 옵션입니다: " + productOptionId));

        if (productOption.getImageUrl() != null && !productOption.getImageUrl().isEmpty()) {
            // DB에서 이미지 URL 제거 (S3는 별도 관리)
            productOption.removeImageUrl();
            productOptionRepository.save(productOption);

            log.info("Product option image URL removed for option ID: {}", productOptionId);
        }
    }
}
