package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.FinalStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
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

    private final GroupBuyOptionRepository groupBuyOptionRepository;

    /**
     * initialStock 기반 배치 통계 계산
     * 배치용 - 여러 공동구매의 통계를 한번에 계산
     * @param groupBuys
     * @return
     */
    public List<GroupBuyStatistics> calculateBatchStatistics(List<GroupBuy> groupBuys) {
        if (groupBuys.isEmpty()) return List.of();

        List<Long> groupBuyIds = groupBuys.stream()
                .map(GroupBuy::getId)
                .toList();

        // initialStock 기반 배치 조회
        Map<Long, Integer> quantityMap = getTotalSoldQuantitiesByGroupBuyIds(groupBuyIds);

        return groupBuys.stream()
                .map(groupBuy -> {
                    int soldQuantity = quantityMap.getOrDefault(groupBuy.getId(), 0);
                    return calculateStatistics(groupBuy, soldQuantity);
                })
                .toList();
    }

    /**
     * initialStock 기반 단일 통계 계산
     * 단일 공동구매 통계 계산 (실시간용)
     * @param groupBuy
     * @return
     */
    public GroupBuyStatistics calculateSingleStatistics(GroupBuy groupBuy) {
        int soldQuantity = getTotalSoldQuantityByGroupBuyId(groupBuy.getId());
        return calculateStatistics(groupBuy, soldQuantity);
    }

    /**
     * 공통 통계 계산 로직
     * @param groupBuy
     * @param totalSoldQuantity 총 판매량 (initialStock - stock)
     * @return
     */
    private GroupBuyStatistics calculateStatistics(GroupBuy groupBuy, int totalSoldQuantity) {
        int discountRate = calculateFinalDiscountRate(groupBuy.getDiscountStages(), totalSoldQuantity);
        FinalStatus status = (discountRate > 0) ? FinalStatus.SUCCESS : FinalStatus.FAIL;

        log.debug("Calculated statistics for group buy: {} - soldQuantity: {}, discountRate: {}, status: {}",
                groupBuy.getId(), totalSoldQuantity, discountRate, status);

        return GroupBuyStatistics.of(
                groupBuy,
                totalSoldQuantity, // 참여자 수 = 판매량
                totalSoldQuantity, // 총 판매량
                discountRate,
                status,
                Instant.now()
        );
    }

    /**
     * 최종 할인율 계산
     * @param discountStagesJson
     * @param totalSoldQuantity 총 판매량
     * @return
     */
    public int calculateFinalDiscountRate(String discountStagesJson, int totalSoldQuantity) {
        if (totalSoldQuantity <= 0) return 0;

        try {
            List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(discountStagesJson);
            return stages.stream()
                    .filter(stage -> totalSoldQuantity >= stage.minQuantity())
                    .mapToInt(DiscountStageDto::discountRate)
                    .max()
                    .orElse(0);
        } catch (Exception e) {
            log.warn("할인율 계산 실패 - totalSoldQuantity: {}", totalSoldQuantity, e);
            return 0;
        }
    }

    /**
     * initialStock 기반 단일 공동구매의 총 판매량 조회
     * @param groupBuyId
     * @return
     */
    private int getTotalSoldQuantityByGroupBuyId(Long groupBuyId) {
        Integer result = groupBuyOptionRepository.getTotalSoldQuantityByGroupBuyId(groupBuyId);
        int soldQuantity = Optional.ofNullable(result).orElse(0);

        log.debug("Total sold quantity for group buy {}: {}", groupBuyId, soldQuantity);
        return soldQuantity;
    }

    /**
     * initialStock 기반 여러 공동구매 ID의 총 판매량 조회
     * @param groupBuyIds
     * @return
     */
    private Map<Long, Integer> getTotalSoldQuantitiesByGroupBuyIds(List<Long> groupBuyIds) {
        List<Object[]> results = groupBuyOptionRepository.getTotalSoldQuantitiesByGroupBuyIds(groupBuyIds);

        Map<Long, Integer> quantityMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],           // groupBuyId
                        row -> ((Long) row[1]).intValue() // totalSoldQuantity
                ));

        log.debug("Batch sold quantities calculated for {} group buys", groupBuyIds.size());
        return quantityMap;
    }
}
