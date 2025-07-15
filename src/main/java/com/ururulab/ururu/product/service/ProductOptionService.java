package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.image.service.ImageHashService;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import com.ururulab.ururu.product.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.dto.response.ProductOptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionService {

    private final ProductOptionRepository productOptionRepository;
    private final ImageHashService imageHashService;
    private final ProductOptionImageService productOptionImageService;
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public List<ProductOption> saveProductOptions(Product product, List<ProductOptionRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductOption> productOptions = requests.stream()
                .map(request -> request.toEntity(product))
                .toList();

        return productOptionRepository.saveAll(productOptions);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public List<ProductOptionResponse> updateOptions(Product product,
                                                     List<ProductOptionRequest> optionRequests,
                                                     List<MultipartFile> optionImages) {

        // 기존 옵션들 조회
        List<ProductOption> existingOptions = productOptionRepository.findByProductIdAndIsDeletedFalse(product.getId());
        Map<Long, ProductOption> existingOptionsMap = existingOptions.stream()
                .collect(Collectors.toMap(ProductOption::getId, option -> option));

        List<ProductOption> changedOptions = new ArrayList<>();
        List<ProductImageUploadRequest> imageUploadRequests = new ArrayList<>();
        List<String> imagesToDelete = new ArrayList<>();

        // 요청된 옵션들 처리
        for (int i = 0; i < optionRequests.size(); i++) {
            ProductOptionRequest optionRequest = optionRequests.get(i);
            MultipartFile newImage = (optionImages != null && i < optionImages.size()) ? optionImages.get(i) : null;

            if (optionRequest.id() != null && existingOptionsMap.containsKey(optionRequest.id())) {
                // 기존 옵션 업데이트
                ProductOption existingOption = existingOptionsMap.get(optionRequest.id());
                boolean optionChanged = updateExistingOptionIfChanged(existingOption, optionRequest, newImage,
                        imageUploadRequests, imagesToDelete);

                if (optionChanged) {
                    changedOptions.add(existingOption);
                }

            } else {
                // 새 옵션 생성
                ProductOption newOption = createNewOption(product, optionRequest, newImage, imageUploadRequests);
                changedOptions.add(newOption);
                log.info("Creating new option: {} for product: {}", optionRequest.name(), product.getId());
            }
        }

        // 삭제된 옵션들 처리
        deleteRemovedOptionsFromUpdate(product.getId(), optionRequests, existingOptions);


        // 변경된 옵션들만 저장
        if (!changedOptions.isEmpty()) {
            productOptionRepository.saveAll(changedOptions);
            log.info("Updated {} options for product: {}", changedOptions.size(), product.getId());
        }

        // 비동기 이미지 업로드/삭제 이벤트 발행 (ProductOptionImageService에 위임)
        productOptionImageService.publishImageEvents(product.getId(), imageUploadRequests, imagesToDelete);

        // 최종 옵션 목록 반환
        return productOptionRepository.findByProductIdAndIsDeletedFalse(product.getId())
                .stream()
                .map(ProductOptionResponse::from)
                .toList();
    }

    /**
     * 업데이트 시 제거된 옵션들을 삭제 처리하는 메서드
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteRemovedOptionsFromUpdate(Long productId,
                                               List<ProductOptionRequest> optionRequests,
                                               List<ProductOption> existingOptions) {

        log.info("=== 제거된 옵션 삭제 처리 시작 ===");

        // 빠른 종료 조건
        if (existingOptions.isEmpty()) {
            log.info("기존 옵션이 없어 삭제 처리를 건너뜁니다.");
            return;
        }

        // 요청에 포함된 기존 옵션 ID들 수집
        Set<Long> requestedOptionIds = optionRequests.stream()
                .map(ProductOptionRequest::id)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        log.info("요청에 포함된 기존 옵션 IDs: {}", requestedOptionIds);

        // 삭제 대상 ID만 추출
        List<Long> optionIdsToDelete = new ArrayList<>();
        for (ProductOption option : existingOptions) {
            if (!requestedOptionIds.contains(option.getId())) {
                optionIdsToDelete.add(option.getId());
            }
        }

        if (optionIdsToDelete.isEmpty()) {
            log.info("삭제할 옵션이 없습니다.");
            return;
        }

        log.info("삭제 대상 옵션 IDs: {}", optionIdsToDelete);

        // DB 기반 정확한 검증
        int remainingActiveCount = productOptionRepository.countActiveOptionsExcluding(productId, optionIdsToDelete);

        if (remainingActiveCount < 1) {
            log.error("최소 1개의 옵션은 유지되어야 합니다. 삭제 후 남은 옵션 수: {}", remainingActiveCount);
            throw new BusinessException(ErrorCode.CANNOT_DELETE_LAST_OPTION);
        }

        // 배치 삭제 처리
        int deletedCount = productOptionRepository.softDeleteByIds(optionIdsToDelete, productId);

        log.info("삭제 처리 완료: {}개 옵션 삭제됨", deletedCount);
        log.info("=== 제거된 옵션 삭제 처리 완료 ===");
    }

    /**
     * 특정 옵션들을 삭제하는 메서드
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteOptionsByIds(Long productId, List<Long> optionIdsToDelete) {

        log.info("특정 옵션들 삭제 시작 - Product ID: {}, Option IDs: {}", productId, optionIdsToDelete);

        if (optionIdsToDelete == null || optionIdsToDelete.isEmpty()) {
            log.info("삭제할 옵션 ID가 없습니다.");
            return;
        }

        // 기존 메서드 재사용하여 유효성 검증
        List<ProductOption> existingOptionsToDelete = productOptionRepository.findAllByIdInAndProductId(optionIdsToDelete, productId);

        // 실제 존재하고 삭제되지 않은 옵션들만 필터링
        List<Long> validIdsToDelete = existingOptionsToDelete.stream()
                .filter(option -> !option.getIsDeleted())
                .map(ProductOption::getId)
                .toList();

        if (validIdsToDelete.isEmpty()) {
            log.warn("삭제 가능한 옵션이 없습니다. Product ID: {}, Option IDs: {}", productId, optionIdsToDelete);
            return;
        }

        log.info("유효한 삭제 대상 옵션 IDs: {}", validIdsToDelete);

        // DB 기반 정확한 검증
        int remainingActiveCount = productOptionRepository.countActiveOptionsExcluding(productId, validIdsToDelete);

        if (remainingActiveCount < 1) {
            log.error("최소 1개의 옵션은 유지되어야 합니다. Product ID: {}", productId);
            throw new BusinessException(ErrorCode.CANNOT_DELETE_LAST_OPTION);
        }

        // 배치 삭제 처리
        int deletedCount = productOptionRepository.softDeleteByIds(validIdsToDelete, productId);

        log.info("특정 옵션들 삭제 완료: {}개", deletedCount);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionResponse> getProductOptions(Long productId) {
        return productOptionRepository.findByProductIdAndIsDeletedFalse(productId)
                .stream()
                .map(ProductOptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductOptionResponse> getProductOptions(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ACCESS_DENIED);
        }

        return productOptionRepository.findByProductIdAndIsDeletedFalse(productId)
                .stream()
                .map(ProductOptionResponse::from)
                .toList();
    }

    /**
     * 기존 옵션 업데이트 (이미지 처리 포함)
     */
    private boolean updateExistingOptionIfChanged(ProductOption existingOption, ProductOptionRequest request,
                                                  MultipartFile newImage, List<ProductImageUploadRequest> imageUploadRequests,
                                                  List<String> imagesToDelete) {
        boolean changed = false;

        // 기본 정보 변경 확인
        if (!request.name().equals(existingOption.getName())) {
            existingOption.updateName(request.name());
            changed = true;
            log.info("Option name updated: '{}' -> '{}' for ID: {}", existingOption.getName(), request.name(), existingOption.getId());
        }

        if (!request.price().equals(existingOption.getPrice())) {
            existingOption.updatePrice(request.price());
            changed = true;
            log.info("Option price updated: {} -> {} for ID: {}", existingOption.getPrice(), request.price(), existingOption.getId());
        }

        if (!request.fullIngredients().equals(existingOption.getFullIngredients())) {
            existingOption.updateFullIngredients(request.fullIngredients());
            changed = true;
            log.info("Option ingredients updated for ID: {}", existingOption.getId());
        }

        // 이미지 처리
        if (newImage != null && !newImage.isEmpty()) {
            try {
                String newImageHash = imageHashService.calculateImageHash(newImage);

                if (!newImageHash.equals(existingOption.getImageHash())) {
                    String existingImageUrl = existingOption.getImageUrl();
                    if (existingImageUrl != null) {
                        imagesToDelete.add(existingImageUrl);
                        log.info("Scheduled existing image for deletion: {}", existingImageUrl);
                    }

                    File tempFile = createTempFile(newImage);

                    imageUploadRequests.add(new ProductImageUploadRequest(
                            existingOption.getId(),
                            newImage.getOriginalFilename(),
                            tempFile.getAbsolutePath(),
                            newImageHash
                    ));

                    existingOption.updateImageHash(newImageHash);
                    existingOption.updateImageUrl(null); // 비동기 업로드 완료 후 업데이트
                    changed = true;
                    log.info("Scheduled image upload for option: {}", existingOption.getId());
                } else {
                    log.info("Same image hash, skipping upload for option: {}", existingOption.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process image for option: {}", existingOption.getId(), e);
                throw new BusinessException(ErrorCode.IMAGE_PROCESSING_FAILED);
            }
        }

        return changed;
    }

    /**
     * 새 옵션 생성 (이미지 처리 포함)
     */
    private ProductOption createNewOption(Product product, ProductOptionRequest request,
                                          MultipartFile newImage, List<ProductImageUploadRequest> imageUploadRequests) {

        ProductOption newOption = request.toEntity(product);
        ProductOption savedOption = productOptionRepository.save(newOption);

        if (newImage != null && !newImage.isEmpty()) {
            try {
                String imageHash = imageHashService.calculateImageHash(newImage);
                File tempFile = createTempFile(newImage);

                imageUploadRequests.add(new ProductImageUploadRequest(
                        savedOption.getId(),
                        newImage.getOriginalFilename(),
                        tempFile.getAbsolutePath(),
                        imageHash
                ));

                log.info("Scheduled image upload for new option: {}", savedOption.getId());
            } catch (Exception e) {
                log.error("Failed to process image for new option: {}", savedOption.getId(), e);
                throw new BusinessException(IMAGE_PROCESSING_FAILED);
            }
        }

        return savedOption;
    }

    /**
     * 임시 파일 생성
     */
    private File createTempFile(MultipartFile multipartFile) throws IOException {
        File tempFile = Files.createTempFile(
                "option_" + System.currentTimeMillis() + "_",
                "_" + multipartFile.getOriginalFilename()
        ).toFile();

        multipartFile.transferTo(tempFile);
        tempFile.deleteOnExit();

        log.debug("Created temp file for option: {} (size: {} bytes)",
                tempFile.getName(), tempFile.length());

        return tempFile;
    }

    @Transactional
    public void deleteProductOption(Long productId, Long optionId, Long sellerId) {
        productRepository.findByIdAndSellerIdAndStatusIn(
                productId, sellerId, Arrays.asList(Status.ACTIVE, Status.INACTIVE)
        ).orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));

        ProductOption option = productOptionRepository.findByIdAndIsDeletedFalse(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND, optionId));

        if (!option.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_BELONG_TO_PRODUCT);
        }

        List<ProductOption> activeOptions = productOptionRepository.findByProductIdAndIsDeletedFalse(productId);

        if (activeOptions.size() <= 1) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_LAST_OPTION);
        }

        option.markAsDeleted();
        productOptionRepository.save(option);
    }
}
