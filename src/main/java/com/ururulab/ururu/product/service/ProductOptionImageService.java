package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.image.service.ImageHashService;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.image.validation.ImageValidator;
import com.ururulab.ururu.product.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import com.ururulab.ururu.product.event.ProductImageDeleteEvent;
import com.ururulab.ururu.product.event.ProductImageUploadEvent;
import com.ururulab.ururu.product.service.validation.ProductValidator;
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

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;
import static com.ururulab.ururu.image.domain.ImageCategory.PRODUCTS;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionImageService {
    private final ImageService imageService;
    private final ProductOptionRepository productOptionRepository;
    private final ImageHashService imageHashService;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductValidator productValidator;
    private final ImageValidator imageValidator;


    /**
     * 상품 옵션 이미지 단일 업로드
     */
    public String uploadProductOptionImage(MultipartFile file) {
        try {
            imageValidator.validateImage(file);
            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElseThrow(() -> new BusinessException(INVALID_IMAGE_FILENAME));

            String imageUrl = imageService.uploadImage(
                    PRODUCTS.getPath(),
                    filename,
                    file.getBytes()
            );
            log.info("Product option image uploaded: {}", imageUrl);
            return imageUrl;
        } catch (IOException e) {
            log.error("IO Error while uploading product option image: {}", e.getMessage());
            throw new BusinessException(IMAGE_READ_FAILED);
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
                        .orElseThrow(() -> new BusinessException(
                                PRODUCT_OPTION_NOT_FOUND, imageRequest.productOptionId()));

                option.updateImageInfo(imageUrl, imageRequest.imageHash());
                productOptionRepository.save(option);

                log.info("Image uploaded for option ID: {} -> {}",
                        imageRequest.productOptionId(), imageUrl);

            } catch (Exception e) {
                log.error("Failed to upload image for option ID: {}",
                        imageRequest.productOptionId(), e);
                throw new BusinessException(IMAGE_PROCESSING_FAILED);
            }
        }
    }

    public List<ProductImageUploadRequest> createImageUploadRequests(
            List<ProductOption> savedOptions, List<MultipartFile> optionImages) {

        if (savedOptions.size() != optionImages.size()) {
            throw new BusinessException(ErrorCode.OPTION_IMAGE_COUNT_MISMATCH,
                    savedOptions.size(), optionImages.size());
        }

        List<ProductImageUploadRequest> requests = new ArrayList<>();

        for (int i = 0; i < savedOptions.size(); i++) {
            MultipartFile imageFile = optionImages.get(i);

            if (imageFile == null || imageFile.isEmpty()) {
                log.warn("Image at index {} is null or empty", i);
                continue;
            }

            ProductOption option = savedOptions.get(i);

            try {
                byte[] imageData = imageFile.getBytes();
                String imageHash = imageHashService.calculateImageHashFromBytes(imageData);

                log.info("Image processed - optionId: {}, filename: {}, hash: {}, size: {} bytes",
                        option.getId(), imageFile.getOriginalFilename(), imageHash, imageData.length);

                requests.add(new ProductImageUploadRequest(
                        option.getId(),
                        imageFile.getOriginalFilename(),
                        imageData,
                        imageHash
                ));

            } catch (IOException e) {
                log.error("Failed to read image file for optionId: {}", option.getId(), e);
                throw new BusinessException(IMAGE_READ_FAILED);
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
