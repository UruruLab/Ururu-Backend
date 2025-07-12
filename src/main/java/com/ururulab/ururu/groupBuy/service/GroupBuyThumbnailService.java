package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.event.GroupBuyThumbnailUploadEvent;
import com.ururulab.ururu.image.service.ImageHashService;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.image.validation.ImageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;
import static com.ururulab.ururu.image.domain.ImageCategory.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyThumbnailService {

    private final ImageService imageService;
    private final GroupBuyRepository groupBuyRepository;
    private final ImageHashService imageHashService;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageValidator imageValidator;

    /**
     * 썸네일 업로드 이벤트 발행 (검증 + 임시 파일 생성)
     */
    public void uploadThumbnail(Long groupBuyId, MultipartFile thumbnailFile) {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            log.warn("Thumbnail file is null or empty for groupBuy: {}", groupBuyId);
            return;
        }

        try {
            // 이미지 업로드 시 용량 검증
            imageValidator.validateSingleFileSize(thumbnailFile);

            // 이미지 검증
            imageValidator.validateImage(thumbnailFile);

            String imageHash = imageHashService.calculateImageHash(thumbnailFile);
            File tempFile = createTempFile(thumbnailFile);

            log.info("Thumbnail processed - groupBuyId: {}, filename: {}, hash: {}, size: {} bytes",
                    groupBuyId, thumbnailFile.getOriginalFilename(), imageHash, tempFile.length());

            // 이벤트 발행 (임시 파일 경로 전달)
            eventPublisher.publishEvent(new GroupBuyThumbnailUploadEvent(
                    groupBuyId,
                    thumbnailFile.getOriginalFilename(),
                    tempFile.getAbsolutePath(),
                    imageHash
            ));

            log.info("Scheduled thumbnail for upload for groupBuy: {}", groupBuyId);

        } catch (IOException e) {
            log.error("Failed to process thumbnail file for groupBuy: {}", groupBuyId, e);
            throw new BusinessException(IMAGE_READ_FAILED);
        } catch (Exception e) {
            log.error("Failed to validate thumbnail file for groupBuy: {}", groupBuyId, e);
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        }
    }

    /**
     * 비동기 썸네일 업로드 및 DB 업데이트 (스트리밍 방식)
     */
    @Async("imageUploadExecutor")
    @Transactional
    public void uploadThumbnailAsync(Long groupBuyId, String originalFilename, String tempFilePath, String imageHash) {
        log.info("Processing thumbnail upload for groupBuy: {}", groupBuyId);

        File tempFile = new File(tempFilePath);

        try {
            // 재시도 메커니즘이 적용된 S3 업로드
            String imageUrl = uploadToS3WithRetry(tempFile, originalFilename, groupBuyId);

            // DB 업데이트
            updateGroupBuyThumbnail(groupBuyId, imageUrl, imageHash);

            log.info("Thumbnail successfully uploaded for groupBuy ID: {} -> {}", groupBuyId, imageUrl);

        } catch (Exception e) {
            log.error("Final failure: thumbnail upload for groupBuy ID: {}", groupBuyId, e);
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 임시 파일 생성
     */
    private File createTempFile(MultipartFile multipartFile) throws IOException {
        File tempFile = Files.createTempFile(
                "thumbnail_" + System.currentTimeMillis() + "_",
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
     * 재시도 메커니즘이 적용된 S3 업로드
     */
    @Retryable(
            value = {S3Exception.class, SocketTimeoutException.class, ConnectException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    private String uploadToS3WithRetry(File tempFile, String originalFilename, Long groupBuyId) {
        log.info("Attempting S3 upload for groupBuy: {} (file: {})", groupBuyId, originalFilename);

        try {
            String imageUrl = imageService.uploadFileStreaming(
                    tempFile,
                    originalFilename,
                    GROUPBUY_THUMBNAIL.getPath()
            );

            log.info("S3 upload successful for groupBuy: {}", groupBuyId);
            return imageUrl;

        } catch (S3Exception e) {
            log.warn("S3 upload failed for groupBuy: {} - S3 Error: {}", groupBuyId, e.getMessage());
            throw e; // 재시도를 위해 예외 재발생
        } catch (Exception e) {
            log.warn("S3 upload failed for groupBuy: {} - Network Error: {}", groupBuyId, e.getMessage());
            throw new RuntimeException("S3 업로드 네트워크 오류", e);
        }
    }

    /**
     * 재시도 최종 실패 시 실행되는 복구 메서드
     */
    @Recover
    private String recoverFromS3UploadFailure(Exception ex, File tempFile, String originalFilename, Long groupBuyId) {
        log.error("S3 upload final failure after all retries for groupBuy: {} - {}",
                groupBuyId, ex.getMessage());

        throw new BusinessException(IMAGE_UPLOAD_FAILED,
                String.format("썸네일 업로드 최종 실패 (groupBuy: %d): %s", groupBuyId, ex.getMessage()));
    }

    /**
     * GroupBuy 썸네일 정보 업데이트
     */
    private void updateGroupBuyThumbnail(Long groupBuyId, String imageUrl, String imageHash) {
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

        groupBuy.updateThumbnailInfo(imageUrl, imageHash);
        groupBuyRepository.save(groupBuy);

        log.info("Updated thumbnail info in DB for groupBuy: {}", groupBuyId);
    }
}
