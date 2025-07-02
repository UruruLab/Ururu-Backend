package com.ururulab.ururu.groupBuy.service.validation;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.controller.dto.common.DiscountStageDto;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;
import static com.ururulab.ururu.groupBuy.controller.dto.validation.GroupBuyValidationConstants.*;

@Component
public class GroupBuyDiscountStageValidator {

    // 할인 단계 개별 검증
    public void validateDiscountStage(DiscountStageDto discountStage) {
        if (discountStage == null) {
            throw new BusinessException(MIN_QUANTITY_REQUIRED);
        }

        validateMinQuantity(discountStage.minQuantity());
        validateDiscountRate(discountStage.discountRate());
    }

    // 할인 단계 전체 검증
    public void validateDiscountStages(List<DiscountStageDto> discountStages) {
        if (discountStages == null || discountStages.isEmpty()) {
            throw new BusinessException(MIN_QUANTITY_REQUIRED);
        }

        discountStages.forEach(this::validateDiscountStage);
        validateDiscountStageRelationships(discountStages);
    }

    // 최소 수량 검증
    private void validateMinQuantity(Integer minQuantity) {
        if (minQuantity == null) {
            throw new BusinessException(MIN_QUANTITY_REQUIRED);
        }

        if (minQuantity < 1 || minQuantity > LIMIT_QUANTITY_MAX) {
            throw new BusinessException(MIN_QUANTITY_OUT_OF_RANGE);
        }
    }

    // 할인율 검증
    private void validateDiscountRate(Integer discountRate) {
        if (discountRate == null) {
            throw new BusinessException(DISCOUNT_RATE_REQUIRED);
        }

        if (discountRate < 0 || discountRate > 100) {
            throw new BusinessException(DISCOUNT_RATE_OUT_OF_RANGE);
        }
    }

    // 할인 단계 간 관계 검증
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

    // 재고 대비 최소 수량 검증
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
}
