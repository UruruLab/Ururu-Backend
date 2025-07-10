package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyRankingService {

    private final GroupBuyOptionRepository groupBuyOptionRepository;

    /**
     * 여러 공동구매의 판매량 조회
     * @param groupBuyIds 공동구매 ID 리스트
     * @return 공동구매별 판매량 Map
     */
    public Map<Long, Integer> getOrderCounts(List<Long> groupBuyIds) {
        log.debug("Retrieving sold quantities for {} group buys directly from DB", groupBuyIds.size());

        List<Object[]> results = groupBuyOptionRepository.getTotalSoldQuantitiesByGroupBuyIds(groupBuyIds);

        Map<Long, Integer> resultMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],           // groupBuyId
                        row -> ((Long) row[1]).intValue() // totalSoldQuantity
                ));

        log.debug("Successfully retrieved sold quantities for {} group buys", groupBuyIds.size());
        return resultMap;
    }

    /**
     * 단일 공동구매 판매량 조회
     *
     * @param groupBuyId 공동구매 ID
     * @return 판매량
     */
    public Integer getSoldQuantity(Long groupBuyId) {
        Integer result = groupBuyOptionRepository.getTotalSoldQuantityByGroupBuyId(groupBuyId);
        log.debug("Sold quantity for group buy {}: {}", groupBuyId, result);
        return result != null ? result : 0;
    }
}
