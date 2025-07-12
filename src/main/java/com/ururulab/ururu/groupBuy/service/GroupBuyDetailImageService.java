package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyImageUploadRequest;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyDetailImageRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.event.GroupBuyDetailImageUploadEvent;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;
import static com.ururulab.ururu.groupBuy.dto.validation.GroupBuyValidationConstants.MAX_GROUP_BUY_DETAIL_IMAGES;
import static com.ururulab.ururu.image.domain.ImageCategory.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyDetailImageService {

    private final ImageService imageService;
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyDetailImageRepository groupBuyDetailImageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageHashService imageHashService;
    private final ImageValidator imageValidator;

    /**
     * 상세이미지 업로드 이벤트 발행 (검증 + 임시 파일 생성)
     */
    public void uploadDetailImages(Long groupBuyId, List<MultipartFile> detailImageFiles) {
        if (detailImageFiles == null || detailImageFiles.isEmpty()) {
            log.warn("Detail image files are null or empty for groupBuy: {}", groupBuyId);
            return;
        }

        // 개수 체크
        if (detailImageFiles.size() > MAX_GROUP_BUY_DETAIL_IMAGES) {
            throw new BusinessException(GROUPBUY_DETAIL_IMAGES_TOO_MANY);
        }

        // 이미지 업로드 시 용량 검증
        imageValidator.validateFileSizes(detailImageFiles);

        // 이미지 검증
        imageValidator.validateAllImages(detailImageFiles);

        List<GroupBuyImageUploadRequest> imageRequests = createDetailImageRequests(detailImageFiles);

        if (!imageRequests.isEmpty()) {
            eventPublisher.publishEvent(new GroupBuyDetailImageUploadEvent(groupBuyId, imageRequests));
            log.info("Scheduled {} detail images for upload for groupBuy: {}",
                    imageRequests.size(), groupBuyId);
        }
    }

    /**
     * MultipartFile 리스트 → GroupBuyImageUploadRequest 리스트 변환 (임시 파일 기반)
     */
    private List<GroupBuyImageUploadRequest> createDetailImageRequests(List<MultipartFile> detailImageFiles) {
        AtomicInteger displayOrder = new AtomicInteger(1);
        List<GroupBuyImageUploadRequest> uploadRequests = new ArrayList<>();

        for (MultipartFile file : detailImageFiles) {
            if (file != null && !file.isEmpty()) {
                try {
                    String imageHash = imageHashService.calculateImageHash(file);
                    File tempFile = createTempFile(file);
                    int order = displayOrder.getAndIncrement();

                    log.info("Detail image processed - index: {}, filename: {}, hash: {}, size: {} bytes",
                            order, file.getOriginalFilename(), imageHash, tempFile.length());

                    uploadRequests.add(new GroupBuyImageUploadRequest(
                            null, // groupBuyId - 이벤트에서 관리
                            null, // groupBuyImageId - 새로 생성이므로 null
                            file.getOriginalFilename(),
                            tempFile.getAbsolutePath(), // 임시 파일 경로
                            order, // displayOrder: 1, 2, 3, ..., 10
                            imageHash
                    ));

                } catch (Exception e) {
                    log.error("Failed to process detail image file: {}", file.getOriginalFilename(), e);
                    throw new BusinessException(IMAGE_READ_FAILED);
                }
            }
        }

        return uploadRequests;
    }

    /**
     * 비동기 상세이미지 업로드 및 DB 업데이트 (스트리밍 방식)
     */
    @Async("imageUploadExecutor")
    @Transactional
    public void uploadDetailImagesAsync(Long groupBuyId, List<GroupBuyImageUploadRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        log.info("Processing {} detail images for groupBuy: {}", images.size(), groupBuyId);

        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

        List<GroupBuyImage> detailImages = new ArrayList<>();

        for (GroupBuyImageUploadRequest imageRequest : images) {
            File tempFile = new File(imageRequest.tempFilePath());

            try {
                // File에서 직접 스트리밍 업로드
                String imageUrl = imageService.uploadFileStreaming(
                        tempFile,
                        imageRequest.originalFilename(),
                        GROUPBUY_DETAIL.getPath()
                );

                log.info("Detail image uploaded for groupBuy ID: {} -> {} (order: {})",
                        groupBuyId, imageUrl, imageRequest.displayOrder());

                // 엔티티 생성
                GroupBuyImage detailImage = GroupBuyImage.of(
                        groupBuy,
                        imageUrl,
                        imageRequest.displayOrder(),
                        false
                );

                detailImage.updateImageHash(imageUrl, imageRequest.detailImageHash());
                detailImages.add(detailImage);

            } catch (Exception e) {
                log.error("Failed to upload detail image for groupBuy ID: {} (order: {})",
                        groupBuyId, imageRequest.displayOrder(), e);
                throw new BusinessException(IMAGE_PROCESSING_FAILED);
            } finally {
                // 임시 파일 정리
                cleanupTempFile(tempFile);
            }
        }

        // DB에 저장
        groupBuyDetailImageRepository.saveAll(detailImages);
        log.info("Saved {} detail images to DB for groupBuy: {}", detailImages.size(), groupBuyId);
    }

    /**
     * 임시 파일 생성
     */
    private File createTempFile(MultipartFile multipartFile) throws IOException {
        File tempFile = Files.createTempFile(
                "detail_" + System.currentTimeMillis() + "_",
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
