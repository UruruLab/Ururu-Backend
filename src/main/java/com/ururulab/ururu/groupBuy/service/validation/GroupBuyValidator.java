package com.ururulab.ururu.groupBuy.service.validation;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.controller.dto.request.GroupBuyOptionRequest;
import com.ururulab.ururu.groupBuy.controller.dto.request.GroupBuyRequest;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupBuyValidator {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyDiscountStageValidator discountStageValidator;

    public void validateCritical(GroupBuyRequest request) {
        log.info("🔍 [START] validateCritical - productId: {}", request.productId());

        try {
            log.info("📝 [STEP 1] validateSchedule 시작");
            validateSchedule(request.startAt(), request.endsAt());
            log.info("✅ [STEP 1] validateSchedule 완료");

            log.info("📝 [STEP 2] validateDiscountStages 시작");
            discountStageValidator.validateDiscountStages(request.discountStages());
            log.info("✅ [STEP 2] validateDiscountStages 완료");

            log.info("📝 [STEP 3] totalStock 계산 시작");
            int totalStock = request.options().stream()
                    .mapToInt(GroupBuyOptionRequest::stock)
                    .sum();
            log.info("✅ [STEP 3] totalStock 계산 완료: {}", totalStock);

            log.info("📝 [STEP 4] validateMinQuantityAgainstStock 시작");
            discountStageValidator.validateMinQuantityAgainstStock(request.discountStages(), totalStock);
            log.info("✅ [STEP 4] validateMinQuantityAgainstStock 완료");

            log.info("📝 [STEP 5] 중복 공구 체크 시작 - productId: {}, status: {}",
                    request.productId(), GroupBuyStatus.CLOSED);

            boolean exists = groupBuyRepository.existsByProductIdAndStatusNot(request.productId(), GroupBuyStatus.CLOSED);

            log.info("🔍 [STEP 5] DB 조회 결과 - exists: {}", exists);

            if (exists) {
                log.error("💥 [ERROR] OVERLAPPING_GROUP_BUY_EXISTS 에러 발생! productId: {}", request.productId());
                throw new BusinessException(OVERLAPPING_GROUP_BUY_EXISTS);
            }

            log.info("✅ [STEP 5] 중복 공구 체크 완료 - 중복 없음");
            log.info("🎉 [END] validateCritical 모든 검증 완료!");

        } catch (BusinessException e) {
            log.error("💥 [BUSINESS_ERROR] validateCritical 실패 - 에러코드: {}, 메시지: '{}'",
                    e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("💥 [UNEXPECTED_ERROR] validateCritical 예상치 못한 에러: {}", e.getMessage(), e);
            throw e;
        }
    }
//    public void validateCritical(GroupBuyRequest request) {
//        validateSchedule(request.startAt(), request.endsAt());
//        discountStageValidator.validateDiscountStages(request.discountStages());
//
//        int totalStock = request.options().stream()
//                .mapToInt(GroupBuyOptionRequest::stock)
//                .sum();
//
//        discountStageValidator.validateMinQuantityAgainstStock(request.discountStages(), totalStock);
//
//        //if (groupBuyRepository.existsGroupBuyByProduct(request.productId())) {
//        if (groupBuyRepository.existsByProductIdAndStatusNot(request.productId(), GroupBuyStatus.CLOSED)) {
//            throw new BusinessException(OVERLAPPING_GROUP_BUY_EXISTS);
//        }
//
//    }

    /**
     * 공동구매 스케줄 검증 (핵심 비즈니스 규칙)
     */
    private void validateSchedule(Instant startAt, Instant endsAt) {
        Instant now = Instant.now();

        // 시작일이 현재 시간보다 이전인지 검증
        if (startAt.isBefore(now)) {
            log.error("공동구매 시작일이 현재 시간보다 이전입니다 - startAt: {}", startAt);
            throw new BusinessException(INVALID_START_TIME);
        }

        // 종료일이 시작일보다 이후인지 검증 (DTO에서도 검증하지만 한번 더)
        if (endsAt.isBefore(startAt) || endsAt.equals(startAt)) {
            log.error("공동구매 종료일이 시작일보다 이전이거나 같습니다 - startAt: {}, endAt: {}", startAt, endsAt);
            throw new BusinessException(INVALID_END_TIME);
        }

        final long MIN_DURATION_HOURS = 1;
        final long MAX_DURATION_HOURS = Duration.ofDays(7).toHours(); // 1주일

        long durationHours = Duration.between(startAt, endsAt).toHours();

        if (durationHours < MIN_DURATION_HOURS) {
            throw new BusinessException(GROUP_BUY_DURATION_TOO_SHORT);
        }
        if (durationHours > MAX_DURATION_HOURS) {
            throw new BusinessException(GROUP_BUY_DURATION_TOO_LONG);
        }

    }

    /**
     *  공동구매 이미지 검증
     */
    public void validateImage(MultipartFile image) {
        long start = System.currentTimeMillis();

        if (image == null || image.isEmpty()) {
            return;
        }

        ImageFormat extFmt = parseExtension(image);
        ImageFormat mimeFmt = parseMimeType(image);
        ensureMatchingFormats(extFmt, mimeFmt, image);

        long end = System.currentTimeMillis();
        log.info("Image validation took: {}ms for file: {}", end - start, image.getOriginalFilename());
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
        if (!extFmt.getMimeType().equals(mimeFmt.getMimeType())) {
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
}
