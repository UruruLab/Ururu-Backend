package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.event.GroupBuyThumbnailUploadEvent;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
import com.ururulab.ururu.image.service.ImageHashService;
import com.ururulab.ururu.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
    private final GroupBuyValidator groupBuyValidator;

    /**
     * 썸네일 업로드 이벤트 발행 (비동기 처리용)
     */
    public void uploadThumbnail(Long groupBuyId, MultipartFile thumbnailFile) {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            log.warn("Thumbnail file is null or empty for groupBuy: {}", groupBuyId);
            return;
        }

        try {
            groupBuyValidator.validateImage(thumbnailFile);

            byte[] imageData = thumbnailFile.getBytes();
            String imageHash = imageHashService.calculateImageHashFromBytes(imageData);

            log.info("Thumbnail processed - groupBuyId: {}, filename: {}, hash: {}, size: {} bytes",
                    groupBuyId, thumbnailFile.getOriginalFilename(), imageHash, imageData.length);

            // 이벤트 발행
            eventPublisher.publishEvent(new GroupBuyThumbnailUploadEvent(
                    groupBuyId,
                    thumbnailFile.getOriginalFilename(),
                    imageData,
                    imageHash
            ));

            log.info("Scheduled thumbnail for upload for groupBuy: {}", groupBuyId);

        } catch (IOException e) {
            log.error("Failed to read thumbnail file for groupBuy: {}", groupBuyId, e);
            throw new BusinessException(IMAGE_READ_FAILED);
        }
    }

    /**
     * 비동기 썸네일 업로드 및 DB 업데이트
     */
    @Async("imageUploadExecutor")
    @Transactional
    public void uploadThumbnailAsync(Long groupBuyId, String originalFilename, byte[] data, String imageHash) {
        log.info("Processing thumbnail upload for groupBuy: {}", groupBuyId);

        try {
            // S3에 썸네일 업로드
            String imageUrl = imageService.uploadImage(
                    GROUPBUY_THUMBNAIL.getPath(),
                    originalFilename,
                    data
            );

            // DB 업데이트
            GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                    .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

            groupBuy.updateThumbnailInfo(imageUrl, imageHash);
            groupBuyRepository.save(groupBuy);

            log.info("Thumbnail uploaded for groupBuy ID: {} -> {}", groupBuyId, imageUrl);

        } catch (Exception e) {
            log.error("Failed to upload thumbnail for groupBuy ID: {}", groupBuyId, e);
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        }
    }
}
