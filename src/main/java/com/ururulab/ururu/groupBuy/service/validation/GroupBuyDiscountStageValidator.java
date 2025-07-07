package com.ururulab.ururu.groupBuy.service.validation;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;
import static com.ururulab.ururu.groupBuy.dto.validation.GroupBuyValidationConstants.*;

@Component
@Slf4j
public class GroupBuyDiscountStageValidator {

    /**
     * 할인 단계 개별 검증
     * @param discountStage
     */
    public void validateDiscountStage(DiscountStageDto discountStage) {
        if (discountStage == null) {
            throw new BusinessException(MIN_QUANTITY_REQUIRED);
        }

        validateMinQuantity(discountStage.minQuantity());
        validateDiscountRate(discountStage.discountRate());
    }

    /**
     * 할인 단계 전체 검증
     * @param discountStages
     */
    public void validateDiscountStages(List<DiscountStageDto> discountStages) {
        if (discountStages == null || discountStages.isEmpty()) {
            throw new BusinessException(MIN_QUANTITY_REQUIRED);
        }

        discountStages.forEach(this::validateDiscountStage);
        validateDiscountStageRelationships(discountStages);
    }

    /**
     * 최소 수량 검증
     * @param minQuantity
     */
    private void validateMinQuantity(Integer minQuantity) {
        if (minQuantity == null) {
            throw new BusinessException(MIN_QUANTITY_REQUIRED);
        }

        if (minQuantity < 1 || minQuantity > LIMIT_QUANTITY_MAX) {
            throw new BusinessException(MIN_QUANTITY_OUT_OF_RANGE);
        }
    }

    /**
     * 할인율 검증
     * @param discountRate
     */
    private void validateDiscountRate(Integer discountRate) {
        if (discountRate == null) {
            throw new BusinessException(DISCOUNT_RATE_REQUIRED);
        }

        if (discountRate < 0 || discountRate > 100) {
            throw new BusinessException(DISCOUNT_RATE_OUT_OF_RANGE);
        }
    }

    /**
     * 할인 단계 간 관계 검증
     * @param discountStages
     */
    private void validateDiscountStageRelationships(List<DiscountStageDto> discountStages) {
        long distinctMinQuantities = discountStages.stream()
                .mapToInt(DiscountStageDto::minQuantity)
                .distinct()
                .count();

        if (distinctMinQuantities != discountStages.size()) {
            throw new BusinessException(DUPLICATE_DISCOUNT_STAGE);
        }

        if (discountStages.size() > 10) {
            throw new BusinessException(EXCEEDED_DISCOUNT_STAGE_LIMIT);
        }
    }

    /**
     * 재고 대비 최소 수량 검증
     * @param discountStages
     * @param availableStock
     */
    public void validateMinQuantityAgainstStock(List<DiscountStageDto> discountStages, Integer availableStock) {
        if (availableStock == null || availableStock <= 0) {
            return;
        }

        boolean hasUnreachableStage = discountStages.stream()
                .anyMatch(stage -> stage.minQuantity() > availableStock);

        if (hasUnreachableStage) {
            throw new BusinessException(DISCOUNT_STAGE_EXCEEDS_STOCK);
        }
    }

    /**
     * 리워드 단계 오름차순 순서 검증
     * @param discountStages
     */
    public void validateDiscountStageOrder(List<DiscountStageDto> discountStages) {
        if (discountStages.size() <= 1) {
            return;
        }

        for (int i = 0; i < discountStages.size() - 1; i++) {
            DiscountStageDto current = discountStages.get(i);
            DiscountStageDto next = discountStages.get(i + 1);

            // 최소 달성 수량 오름차순 검증
            if (current.minQuantity() >= next.minQuantity()) {
                throw new BusinessException(DISCOUNT_STAGE_QUANTITY_ORDER_INVALID);
            }

            // 할인률 오름차순 검증
            if (current.discountRate() >= next.discountRate()) {
                throw new BusinessException(DISCOUNT_STAGE_RATE_ORDER_INVALID
                );
            }
        }

        log.debug("할인 단계 순서 검증 성공: {} 단계", discountStages.size());
    }
}
