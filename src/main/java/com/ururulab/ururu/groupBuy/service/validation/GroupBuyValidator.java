package com.ururulab.ururu.groupBuy.service.validation;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.controller.dto.request.GroupBuyOptionRequest;
import com.ururulab.ururu.groupBuy.controller.dto.request.GroupBuyRequest;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupBuyValidator {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyDiscountStageValidator discountStageValidator;

    public void validateCritical(GroupBuyRequest request) {
        validateSchedule(request.startAt(), request.endsAt());
        discountStageValidator.validateDiscountStages(request.discountStages());

        int totalStock = request.options().stream()
                .mapToInt(GroupBuyOptionRequest::stock)
                .sum();

        discountStageValidator.validateMinQuantityAgainstStock(request.discountStages(), totalStock);

        //if (groupBuyRepository.existsGroupBuyByProduct(request.productId())) {
        if (groupBuyRepository.existsByProductIdAndStatusNot(request.productId(), GroupBuyStatus.CLOSED)) {
            throw new BusinessException(OVERLAPPING_GROUP_BUY_EXISTS);
        }

    }

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

}
