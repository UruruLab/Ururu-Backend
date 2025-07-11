package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyPriceService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;

    /**
     * 공동 구매의 displayFinalPrice 계산
     * 공동 구매 가격 = 첫번째 옵션의 공구 시작가 - 리워드의 최대 할인률
     * @param groupBuyId
     */
    @Transactional
    public void updateDisplayFinalPrice(Long groupBuyId) {
        log.debug("Calculating display final price for group buy: {}", groupBuyId);

        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);

        //Integer originalPrice = options.get(0).getPriceOverride();
        Integer originalPrice = options.stream()
                .map(GroupBuyOption::getPriceOverride)
                .min(Integer::compareTo)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NO_OPTIONS));

        Integer maxDiscountRate = getMaxDiscountRate(groupBuy.getDiscountStages());
        Integer finalPrice = originalPrice * (100 - maxDiscountRate) / 100;

        groupBuy.updateDisplayFinalPrice(finalPrice);

        log.debug("Display final price calculated: {} -> {}", originalPrice, finalPrice);
    }

    /**
     * 리워드의 최대 할인률 추출
     * @param discountStagesJson
     * @return
     */
    private Integer getMaxDiscountRate(String discountStagesJson) {
        List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(discountStagesJson);
        return stages.stream()
                .mapToInt(DiscountStageDto::discountRate)
                .max()
                .orElse(0);
    }

    /**
     * 최종 할인율로 모든 옵션의 판매가 업데이트
     * 공동구매 종료 시 최종 할인율 적용
     *
     * @param groupBuy 대상 공동구매
     * @param finalDiscountRate 최종 할인율 (%)
     */
    @Transactional
    public void updateFinalSalePrices(GroupBuy groupBuy, Integer finalDiscountRate) {
        log.debug("Updating final sale prices for group buy: {} with discount rate: {}%",
                groupBuy.getId(), finalDiscountRate);

        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);

        if (options.isEmpty()) {
            log.warn("No options found for group buy: {}", groupBuy.getId());
            return;
        }

        // 각 옵션별 최종 판매가 계산 및 업데이트
        for (GroupBuyOption option : options) {
            // 반올림을 위해 double 계산 후 변환
            int discountAmount = (int) Math.round(option.getPriceOverride() * finalDiscountRate / 100.0);
            int finalPrice = option.getPriceOverride() - discountAmount;

            // 최종 가격이 음수가 되지 않도록 보장
            finalPrice = Math.max(finalPrice, 0);

            option.updateSalePrice(finalPrice);

            log.trace("Option {} price updated: {} -> {} ({}% discount)",
                    option.getId(), option.getPriceOverride(), finalPrice, finalDiscountRate);
        }

        // 변경된 옵션들 일괄 저장
        groupBuyOptionRepository.saveAll(options);

        log.info("Updated final sale prices for {} options with {}% discount",
                options.size(), finalDiscountRate);
    }
}
