package com.ururulab.ururu.product.service;

import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.product.domain.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import com.ururulab.ururu.product.event.ProductImageDeleteEvent;
import com.ururulab.ururu.product.event.ProductImageUploadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ururulab.ururu.image.domain.ImageCategory.PRODUCTS;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionImageService {
    private final ImageService imageService;
    private final ProductOptionRepository productOptionRepository;
    private final ImageHashService imageHashService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 상품 옵션 이미지 검증
     */
    public void validateImage(MultipartFile image) {
        long start = System.currentTimeMillis();
        if (image == null || image.isEmpty()) {
            return;
        }
        validateSingleImage(image);
        long end = System.currentTimeMillis();
        log.info("Image validation took: {}ms for file: {}", end - start, image.getOriginalFilename());
    }

    private void validateSingleImage(MultipartFile file) {
        ImageFormat extFmt = parseExtension(file);
        ImageFormat mimeFmt = parseMimeType(file);
        ensureMatchingFormats(extFmt, mimeFmt, file);
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

    /**
     * 상품 옵션 이미지 단일 업로드
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
     * 비동기 이미지 업로드 및 DB 업데이트
     */
    @Async("imageUploadExecutor")
    @Transactional
    public void uploadImagesAsync(Long productId, List<ProductImageUploadRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        log.info("Processing {} images for product: {}", images.size(), productId);

        for (ProductImageUploadRequest imageRequest : images) {
            try {
                // S3에 이미지 업로드
                String imageUrl = imageService.uploadImage(
                        PRODUCTS.getPath(),
                        imageRequest.originalFilename(),
                        imageRequest.data()
                );

                // DB 업데이트
                ProductOption option = productOptionRepository.findById(imageRequest.productOptionId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "존재하지 않는 상품 옵션입니다: " + imageRequest.productOptionId()));

                //option.updateImageUrl(imageUrl);
                option.updateImageInfo(imageUrl, imageRequest.imageHash());
                productOptionRepository.save(option);

                log.info("Image uploaded for option ID: {} -> {}",
                        imageRequest.productOptionId(), imageUrl);

            } catch (Exception e) {
                log.error("Failed to upload image for option ID: {}",
                        imageRequest.productOptionId(), e);
            }
        }
    }

    // 🆕 이 메서드 추가
    public List<ProductImageUploadRequest> createImageUploadRequests(
            List<ProductOption> savedOptions, List<MultipartFile> optionImages) {

        List<ProductImageUploadRequest> requests = new ArrayList<>();

        for (int i = 0; i < savedOptions.size() && i < optionImages.size(); i++) {
            MultipartFile imageFile = optionImages.get(i);

            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }

            ProductOption option = savedOptions.get(i);

            try {
                String imageHash = imageHashService.calculateImageHash(imageFile);
                requests.add(new ProductImageUploadRequest(
                        option.getId(),
                        imageFile.getOriginalFilename(),
                        imageFile.getBytes(),
                        imageHash
                ));
            } catch (IOException e) {
                log.error("Failed to read image file for option: {}", option.getId(), e);
                throw new RuntimeException("이미지 파일 읽기 실패" + e.getMessage(), e);
            }
        }

        return requests;
    }

    public void publishImageEvents(Long productId, List<ProductImageUploadRequest> imageUploadRequests,
                                   List<String> imagesToDelete) {
        if (!imageUploadRequests.isEmpty()) {
            eventPublisher.publishEvent(new ProductImageUploadEvent(productId, imageUploadRequests));
            log.info("Scheduled {} images for upload", imageUploadRequests.size());
        }

        if (!imagesToDelete.isEmpty()) {
            eventPublisher.publishEvent(new ProductImageDeleteEvent(productId, imagesToDelete));
            log.info("Scheduled {} images for deletion: {}", imagesToDelete.size(), imagesToDelete);
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
     * 상품 옵션 이미지 URL만 업데이트
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
