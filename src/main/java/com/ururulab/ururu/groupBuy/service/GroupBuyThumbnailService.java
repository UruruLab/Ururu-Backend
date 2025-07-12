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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
            // File에서 직접 스트리밍 업로드
            String imageUrl = imageService.uploadFileStreaming(
                    tempFile,
                    originalFilename,
                    GROUPBUY_THUMBNAIL.getPath()
            );

            // DB 업데이트
            GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                    .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

            groupBuy.updateThumbnailInfo(imageUrl, imageHash);
            groupBuyRepository.save(groupBuy);

            log.info("Thumbnail uploaded for groupBuy ID: {} -> {}", groupBuyId, imageUrl);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during thumbnail upload for groupBuy ID: {}", groupBuyId, e);
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        } finally {
            // 임시 파일 정리
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
}
