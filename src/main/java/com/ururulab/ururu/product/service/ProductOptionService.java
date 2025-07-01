package com.ururulab.ururu.product.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.product.domain.dto.request.ProductImageUploadRequest;
import com.ururulab.ururu.product.domain.dto.request.ProductOptionRequest;
import com.ururulab.ururu.product.domain.dto.response.ProductOptionResponse;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @Transactional(propagation = Propagation.MANDATORY)
    public List<ProductOption> saveProductOptions(Product product, List<ProductOptionRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductOption> productOptions = requests.stream()
                .map(request -> request.toEntity(product))
                .toList();

        return productOptionRepository.saveAll(productOptions);
    }

    @Transactional(propagation = Propagation.MANDATORY)
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
        Set<Long> processedOptionIds = new HashSet<>();

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
                processedOptionIds.add(optionRequest.id());

            } else {
                // 새 옵션 생성
                ProductOption newOption = createNewOption(product, optionRequest, newImage, imageUploadRequests);
                changedOptions.add(newOption);
                log.info("Creating new option: {} for product: {}", optionRequest.name(), product.getId());
            }
        }

        // 변경된 옵션들만 저장
        if (!changedOptions.isEmpty()) {
            productOptionRepository.saveAll(changedOptions);
            log.info("Updated {} options for product: {}", changedOptions.size(), product.getId());
        } else {
            log.info("No options changed for product: {}", product.getId());
        }

        // 이미지 업로드/삭제 이벤트 발행
        productOptionImageService.publishImageEvents(product.getId(), imageUploadRequests, imagesToDelete);

        // 최종 옵션 목록 반환
        return productOptionRepository.findByProductIdAndIsDeletedFalse(product.getId())
                .stream()
                .map(ProductOptionResponse::from)
                .toList();
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
        // sellerId가 productId의 실제 소유자인지 확인
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

    private boolean updateExistingOptionIfChanged(ProductOption existingOption, ProductOptionRequest request,
                                                  MultipartFile newImage, List<ProductImageUploadRequest> imageUploadRequests,
                                                  List<String> imagesToDelete) {
        boolean changed = false;

        // 기본 정보 변경 확인
        if (!request.name().equals(existingOption.getName())) {
            String oldName = existingOption.getName();
            existingOption.updateName(request.name());
            changed = true;
            log.info("Option name updated: '{}' -> '{}' for ID: {}", oldName, request.name(), existingOption.getId());
        }

        if (!request.price().equals(existingOption.getPrice())) {
            Integer oldPrice = existingOption.getPrice();
            existingOption.updatePrice(request.price());
            changed = true;
            log.info("Option price updated: {} -> {} for ID: {}", oldPrice, request.price(), existingOption.getId());
        }

        if (!request.fullIngredients().equals(existingOption.getFullIngredients())) {
            existingOption.updateFullIngredients(request.fullIngredients());
            changed = true;
            log.info("Option ingredients updated for ID: {}", existingOption.getId());
        }

        if (newImage != null && !newImage.isEmpty()) {
            try {
                long start = System.currentTimeMillis();
                String newImageHash = imageHashService.calculateImageHash(newImage);
                long end = System.currentTimeMillis();
                log.info("Hash calculation took: {}ms for file: {}", end - start, newImage.getOriginalFilename());

                if (!newImageHash.equals(existingOption.getImageHash())) {
                    String existingImageUrl = existingOption.getImageUrl();
                    if (existingImageUrl != null) {
                        imagesToDelete.add(existingImageUrl);
                        log.info("Different image detected, scheduled existing image for deletion: {}", existingImageUrl);
                    }

                    imageUploadRequests.add(new ProductImageUploadRequest(
                            existingOption.getId(),
                            newImage.getOriginalFilename(),
                            newImage.getBytes(),
                            newImageHash
                    ));

                    existingOption.updateImageHash(newImageHash);
                    existingOption.updateImageUrl(null);
                    changed = true;
                    log.info("Different image detected, scheduled for upload for option: {}", existingOption.getId());
                } else {
                    log.info("Same image detected (hash match: {}), skipping upload for option: {}",
                            newImageHash, existingOption.getId());
                }
            } catch (IOException e) {
                log.error("Failed to read image file for option: {}", existingOption.getId(), e);
                throw new BusinessException(ErrorCode.IMAGE_READ_FAILED);
            } catch (Exception e) {
                log.error("Failed to calculate image hash for option: {}", existingOption.getId(), e);
                throw new BusinessException(ErrorCode.IMAGE_CONVERSION_FAILED);
            }
        } else {
            log.info("No image provided, keeping existing image for option: {} - {}",
                    existingOption.getName(), existingOption.getImageUrl());
        }

        return changed;
    }

    private ProductOption createNewOption(Product product, ProductOptionRequest request,
                                          MultipartFile newImage, List<ProductImageUploadRequest> imageUploadRequests) {

        ProductOption newOption = request.toEntity(product);
        ProductOption savedOption = productOptionRepository.save(newOption);

        if (newImage != null && !newImage.isEmpty()) {
            try {
                String imageHash = imageHashService.calculateImageHash(newImage);

                imageUploadRequests.add(new ProductImageUploadRequest(
                        savedOption.getId(),
                        newImage.getOriginalFilename(),
                        newImage.getBytes(),
                        imageHash
                ));

                log.info("Image upload scheduled for new option: {} (hash: {})",
                        savedOption.getId(), imageHash);
            } catch (IOException e) {
                throw new BusinessException(IMAGE_READ_FAILED);
            }
        }

        return savedOption;
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

        // 삭제 처리
        option.markAsDeleted();
        productOptionRepository.save(option);
    }

}
