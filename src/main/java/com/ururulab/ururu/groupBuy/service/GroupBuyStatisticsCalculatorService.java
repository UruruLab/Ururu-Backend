package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.FinalStatus;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyStatisticsCalculatorService {

    private final OrderItemRepository orderItemRepository;

    /**
     * 배치용 - 여러 공동구매의 통계를 한번에 계산
     */
    public List<GroupBuyStatistics> calculateBatchStatistics(List<GroupBuy> groupBuys) {
        if (groupBuys.isEmpty()) return List.of();

        List<Long> groupBuyIds = groupBuys.stream()
                .map(GroupBuy::getId)
                .toList();

        Map<Long, Integer> quantityMap = getTotalQuantitiesByGroupBuyIds(groupBuyIds);

        return groupBuys.stream()
                .map(groupBuy -> {
                    int quantity = quantityMap.getOrDefault(groupBuy.getId(), 0);
                    return calculateStatistics(groupBuy, quantity);
                })
                .toList();
    }

    /**
     * 단일 공동구매 통계 계산 (실시간용)
     */
    public GroupBuyStatistics calculateSingleStatistics(GroupBuy groupBuy) {
        int quantity = getTotalQuantityByGroupBuyId(groupBuy.getId());
        return calculateStatistics(groupBuy, quantity);
    }

    /**
     * 공통 통계 계산 로직
     */
    private GroupBuyStatistics calculateStatistics(GroupBuy groupBuy, int totalQuantity) {
        int discountRate = calculateFinalDiscountRate(groupBuy.getDiscountStages(), totalQuantity);
        FinalStatus status = (discountRate > 0) ? FinalStatus.SUCCESS : FinalStatus.FAIL;

        return GroupBuyStatistics.of(
                groupBuy,
                totalQuantity, // 참여자 수 = 주문 수량
                totalQuantity,
                discountRate,
                status,
                Instant.now()
        );
    }

    /**
     * 최종 할인율 계산
     */
    public int calculateFinalDiscountRate(String discountStagesJson, int totalQuantity) {
        if (totalQuantity <= 0) return 0;

        try {
            List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(discountStagesJson);
            return stages.stream()
                    .filter(stage -> totalQuantity >= stage.minQuantity())
                    .mapToInt(DiscountStageDto::discountRate)
                    .max()
                    .orElse(0);
        } catch (Exception e) {
            log.warn("할인율 계산 실패 - totalQuantity: {}", totalQuantity, e);
            return 0;
        }
    }

    /**
     * 단일 공동구매의 총 주문 수량 조회
     */
    private int getTotalQuantityByGroupBuyId(Long groupBuyId) {
        Integer result = orderItemRepository.getTotalQuantityByGroupBuyId(groupBuyId);
        return Optional.ofNullable(result).orElse(0);
    }

    /**
     * 여러 공동구매 ID의 총 주문 수량 조회
     */
    private Map<Long, Integer> getTotalQuantitiesByGroupBuyIds(List<Long> groupBuyIds) {
        List<Object[]> results = orderItemRepository.getTotalQuantitiesByGroupBuyIds(groupBuyIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }
}
