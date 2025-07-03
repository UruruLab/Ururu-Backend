package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyOptionRequest;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class GroupBuyOptionService {

    private final ProductOptionRepository productOptionRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final OrderItemRepository orderItemRepository;


    public void createGroupBuyOptions(GroupBuy groupBuy, List<GroupBuyOptionRequest> optionRequests) {
        for (GroupBuyOptionRequest optionRequest : optionRequests) {
            ProductOption productOption = productOptionRepository.findById(optionRequest.productOptionId())
                    .orElseThrow(() -> new BusinessException(PRODUCT_OPTION_NOT_FOUND));

            GroupBuyOption option = optionRequest.toEntity(groupBuy, productOption);
            groupBuyOptionRepository.save(option);
        }
    }

    @Transactional
    public void calculateAndSetSalePrices(GroupBuy groupBuy) {
        // 1. 주문 총 수량 계산
        Integer totalQuantity = orderItemRepository.getTotalQuantityByGroupBuyId(groupBuy.getId());
        if (totalQuantity == null) totalQuantity = 0;

        // 2. 할인 단계 추출
        List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(groupBuy.getDiscountStages());
        // 최소 수량 기준으로 정렬
        stages.sort(Comparator.comparing(DiscountStageDto::minQuantity));

        int appliedRate = 0;
        for (DiscountStageDto stage : stages) {
            if (totalQuantity >= stage.minQuantity()) {
                appliedRate = stage.discountRate();
            }
        }

        // 3. salePrice 계산 및 저장
        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);
        for (GroupBuyOption option : options) {
            int discount = option.getPriceOverride() * appliedRate / 100;
            int salePrice = option.getPriceOverride() - discount;
            option.updateSalePrice(salePrice);
        }
        groupBuyOptionRepository.saveAll(options);
    }
}
