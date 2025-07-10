package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyOptionRequest;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.PRODUCT_OPTION_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyOptionService {

    private final ProductOptionRepository productOptionRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;

    public void createGroupBuyOptions(GroupBuy groupBuy, List<GroupBuyOptionRequest> optionRequests) {
        for (GroupBuyOptionRequest optionRequest : optionRequests) {
            ProductOption productOption = productOptionRepository.findById(optionRequest.productOptionId())
                    .orElseThrow(() -> new BusinessException(PRODUCT_OPTION_NOT_FOUND));

            GroupBuyOption option = optionRequest.toEntity(groupBuy, productOption);
            groupBuyOptionRepository.save(option);
        }

        log.info("Created {} group buy options for group buy: {}", optionRequests.size(), groupBuy.getId());
    }

    /**
     * 할인가 계산
     * @param groupBuy
     */
    @Transactional
    public void calculateAndSetSalePrices(GroupBuy groupBuy) {
        log.debug("Calculating sale prices for group buy: {}", groupBuy.getId());

        // initialStock 기반 총 판매량 계산
        Integer totalSoldQuantity = groupBuyOptionRepository.getTotalSoldQuantityByGroupBuyId(groupBuy.getId());
        if (totalSoldQuantity == null) totalSoldQuantity = 0;

        log.debug("Total sold quantity for group buy {}: {}", groupBuy.getId(), totalSoldQuantity);

        // 2. 할인 단계 추출 및 정렬
        List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(groupBuy.getDiscountStages());
        stages.sort(Comparator.comparing(DiscountStageDto::minQuantity)); // 최소 수량 기준으로 정렬

        // 3. 현재 판매량에 적용할 할인율 계산
        int appliedRate = calculateAppliedDiscountRate(totalSoldQuantity, stages);

        log.debug("Applied discount rate for group buy {}: {}%", groupBuy.getId(), appliedRate);

        // 4. 모든 옵션의 salePrice 계산 및 저장
        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);
        updateOptionSalePrices(options, appliedRate);

        groupBuyOptionRepository.saveAll(options);

        log.info("Updated sale prices for {} options in group buy: {} with {}% discount",
                options.size(), groupBuy.getId(), appliedRate);
    }

    /**
     * 현재 판매량에 따른 할인율 계산
     * @param totalSoldQuantity 총 판매량
     * @param stages 할인 단계 리스트 (정렬된 상태)
     * @return 적용할 할인율 (%)
     */
    private int calculateAppliedDiscountRate(Integer totalSoldQuantity, List<DiscountStageDto> stages) {
        int appliedRate = 0;

        for (DiscountStageDto stage : stages) {
            if (totalSoldQuantity >= stage.minQuantity()) {
                appliedRate = stage.discountRate();
            } else {
                break; // 정렬되어 있으므로 더 이상 확인할 필요 없음
            }
        }

        return appliedRate;
    }

    /**
     * 옵션들의 판매가 업데이트
     * @param options 업데이트할 옵션 리스트
     * @param discountRate 적용할 할인율 (%)
     */
    private void updateOptionSalePrices(List<GroupBuyOption> options, int discountRate) {
        for (GroupBuyOption option : options) {
            int discount = option.getPriceOverride() * discountRate / 100;
            int salePrice = option.getPriceOverride() - discount;
            option.updateSalePrice(salePrice);

            log.debug("Updated option {} sale price: {} -> {} ({}% discount)",
                    option.getId(), option.getPriceOverride(), salePrice, discountRate);
        }
    }
}
