package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupBuyStockService {

    private final OrderItemRepository orderItemRepository;

    /**
     * 특정 공동구매의 옵션별 실시간 재고 계산
     */
    public Map<Long, Integer> getCurrentStocksByGroupBuy(Long groupBuyId, List<GroupBuyOption> options) {
        // 1. 해당 공동구매의 옵션별 주문량 조회 (1번 쿼리)
        List<Object[]> orderedQuantities = orderItemRepository.getOptionQuantitiesByGroupBuyId(groupBuyId);
        Map<Long, Integer> orderMap = orderedQuantities.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],           // optionId
                        row -> ((Long) row[1]).intValue() // orderedQuantity
                ));

        // 2. 옵션별 실시간 재고 계산
        return options.stream()
                .collect(Collectors.toMap(
                        GroupBuyOption::getId,
                        option -> {
                            Integer originalStock = option.getStock();
                            Integer orderedQuantity = orderMap.getOrDefault(option.getId(), 0);
                            return Math.max(0, originalStock - orderedQuantity); // 음수 방지
                        }
                ));
    }
}
