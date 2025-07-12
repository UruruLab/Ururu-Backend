package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.image.service.ImageHashService;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.image.validation.ImageValidator;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import com.ururulab.ururu.product.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.event.ProductImageDeleteEvent;
import com.ururulab.ururu.product.event.ProductImageUploadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.IMAGE_UPLOAD_FAILED;
import static com.ururulab.ururu.image.domain.ImageCategory.PRODUCTS;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionImageService {

    private final ImageHashService imageHashService;
    private final ImageService imageService;
    private final ProductOptionRepository productOptionRepository;
    private final ImageValidator imageValidator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 검증 후 임시 파일 방식으로 이미지 업로드 요청 생성
     */
    public List<ProductImageUploadRequest> createImageUploadRequests(List<ProductOption> savedOptions,
                                                                     List<MultipartFile> optionImages) {
        if (optionImages == null || optionImages.isEmpty()) {
            return List.of();
        }

        // 이미지 업로드 시 용량 검증
        imageValidator.validateFileSizes(optionImages);

        // 이미지 검증
        imageValidator.validateAllImages(optionImages);

        List<ProductImageUploadRequest> uploadRequests = new ArrayList<>();

        for (int i = 0; i < Math.min(savedOptions.size(), optionImages.size()); i++) {
            ProductOption option = savedOptions.get(i);
            MultipartFile image = optionImages.get(i);

            if (image != null && !image.isEmpty()) {
                try {
                    String imageHash = imageHashService.calculateImageHash(image);
                    File tempFile = createTempFile(image);

                    uploadRequests.add(new ProductImageUploadRequest(
                            option.getId(),
                            image.getOriginalFilename(),
                            tempFile.getAbsolutePath(),
                            imageHash
                    ));

                    log.debug("Created upload request for option: {} (temp file: {})",
                            option.getId(), tempFile.getName());

                } catch (Exception e) {
                    log.error("Failed to create upload request for option: {}", option.getId(), e);
                }
            }
        }

        log.info("Created {} validated image upload requests", uploadRequests.size());
        return uploadRequests;
    }

    /**
     * 비동기 이미지 업로드 처리
     */
    public void uploadImagesAsync(Long productId, List<ProductImageUploadRequest> uploadRequests) {
        log.info("Processing {} image uploads for product: {}", uploadRequests.size(), productId);

        for (ProductImageUploadRequest request : uploadRequests) {
            processImageUpload(request);
        }

        log.info("Completed all image uploads for product: {}", productId);
    }

    /**
     * 개별 이미지 업로드 처리
     */
    private void processImageUpload(ProductImageUploadRequest request) {
        File tempFile = new File(request.tempFilePath());

        try {
            log.debug("Processing image upload for option: {} (file: {})",
                    request.productOptionId(), request.originalFilename());

            // 재시도 적용
            String imageUrl = uploadToS3WithRetry(tempFile, request.originalFilename(), request.productOptionId());

            // DB 업데이트
            updateOptionImage(request.productOptionId(), request.imageHash(), imageUrl);

            log.info("Image uploaded successfully: {} -> {}", request.originalFilename(), imageUrl);

        } catch (Exception e) {
            log.error("Failed to process image upload for option: {}", request.productOptionId(), e);
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * ProductOption 이미지 정보 업데이트
     */
    private void updateOptionImage(Long productOptionId, String imageHash, String imageUrl) {
        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new IllegalStateException("ProductOption not found: " + productOptionId));

        option.updateImageHash(imageHash);
        option.updateImageUrl(imageUrl);
        productOptionRepository.save(option);

        log.debug("Updated option {} with image: {}", productOptionId, imageUrl);
    }

    /**
     * 임시 파일 생성
     */
    private File createTempFile(MultipartFile multipartFile) throws IOException {
        File tempFile = Files.createTempFile(
                "upload_" + System.currentTimeMillis() + "_",
                "_" + multipartFile.getOriginalFilename()
        ).toFile();

        multipartFile.transferTo(tempFile);
        tempFile.deleteOnExit();

        log.debug("Created temp file: {} (size: {} bytes)", tempFile.getName(), tempFile.length());
        return tempFile;
    }

    /**
     * 임시 파일 정리
     */
    private void cleanupTempFile(File tempFile) {
        try {
            if (tempFile.exists() && tempFile.delete()) {
                log.debug("Cleaned up temp file: {}", tempFile.getName());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temp file: {}", tempFile.getName(), e);
        }
    }

    /**
     * 이미지 업로드/삭제 이벤트 발행
     */
    public void publishImageEvents(Long productId, List<ProductImageUploadRequest> imageUploadRequests,
                                   List<String> imagesToDelete) {
        if (!imageUploadRequests.isEmpty()) {
            eventPublisher.publishEvent(new ProductImageUploadEvent(productId, imageUploadRequests));
            log.info("Scheduled {} images for upload", imageUploadRequests.size());
        }

        if (!imagesToDelete.isEmpty()) {
            eventPublisher.publishEvent(new ProductImageDeleteEvent(productId, imagesToDelete));
            log.info("Scheduled {} images for deletion", imagesToDelete.size());
        }
    }

    /**
     * 재시도 메커니즘이 적용된 S3 업로드
     */
    @Retryable(
            value = {S3Exception.class, SocketTimeoutException.class, ConnectException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    private String uploadToS3WithRetry(File tempFile, String originalFilename, Long productOptionId) {
        log.info("Attempting S3 upload for option: {} (file: {})", productOptionId, originalFilename);

        try {
            // File에서 직접 스트리밍 업로드 (메모리에 로드하지 않음)
            String imageUrl = imageService.uploadFileStreaming(tempFile, originalFilename, PRODUCTS.getPath());

            log.info("S3 upload successful for option: {}", productOptionId);
            return imageUrl;

        } catch (S3Exception e) {
            log.warn("S3 upload failed for option: {} - S3 Error: {}", productOptionId, e.getMessage());
            throw e; // 재시도를 위해 예외 재발생
        } catch (Exception e) {
            log.warn("S3 upload failed for option: {} - Network Error: {}", productOptionId, e.getMessage());
            throw new RuntimeException("S3 업로드 네트워크 오류", e);
        }
    }

    /**
     * 재시도 최종 실패 시 실행되는 복구 메서드
     */
    @Recover
    private String recoverFromS3UploadFailure(Exception ex, File tempFile, String originalFilename, Long productOptionId) {
        log.error("S3 upload final failure after all retries for option: {} - {}",
                productOptionId, ex.getMessage());

        throw new BusinessException(IMAGE_UPLOAD_FAILED,
                String.format("상품 옵션 이미지 업로드 최종 실패 (option: %d): %s", productOptionId, ex.getMessage()));
    }
}
