package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyStatisticsRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyStatisticsDetailResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyStatisticsResponse;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyStatisticsService {
    private final GroupBuyStatisticsRepository statisticsRepository;
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final OrderItemRepository orderItemRepository;

    public GroupBuyStatisticsDetailResponse getGroupBuyStatisticsDetail(Long groupBuyId, Long sellerId) {
        // 1. 공동구매 조회 및 본인 소유 확인
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND));

        if (!groupBuy.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ACCESS_DENIED);
        }

        // 2. 통계 데이터 조회
        GroupBuyStatistics statistics = statisticsRepository.findByGroupBuyId(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_STATISTICS_NOT_FOUND));

        // 3. 옵션별 주문 수량 조회
        List<Object[]> optionQuantities = orderItemRepository.getOptionQuantitiesByGroupBuyId(groupBuyId);
        List<Long> optionIds = optionQuantities.stream()
                .map(result -> (Long) result[0])
                .toList();

        // 4. 옵션 이름 조회
        Map<Long, String> optionNameMap = groupBuyOptionRepository.findIdAndNameByIdIn(optionIds)
                .stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (String) result[1]
                ));

        // 5. DTO 변환
        List<GroupBuyStatisticsDetailResponse.GroupBuyOptionOrderInfo> optionOrderInfos = optionQuantities.stream()
                .map(result -> {
                    Long optionId = (Long) result[0];
                    Integer orderCount = ((Number) result[1]).intValue();
                    String optionName = optionNameMap.getOrDefault(optionId, "알 수 없는 옵션");

                    return new GroupBuyStatisticsDetailResponse.GroupBuyOptionOrderInfo(
                            optionId,
                            optionName,
                            orderCount
                    );
                })
                .toList();

        // 6. 응답 생성
        return GroupBuyStatisticsDetailResponse.from(statistics, optionOrderInfos);
    }

    /**
     * 판매자의 모든 그룹 구매 통계 조회
     */
    public List<GroupBuyStatisticsResponse> getGroupBuyStatisticsBySeller(Long sellerId) {
        List<GroupBuyStatistics> statisticsList = statisticsRepository.findByGroupBuy_SellerId(sellerId);

        if (statisticsList.isEmpty()) {
            throw new BusinessException(GROUPBUY_STATISTICS_NOT_FOUND);
        }

        return statisticsRepository.findByGroupBuy_SellerId(sellerId)
                .stream()
                .map(GroupBuyStatisticsResponse::from)
                .toList();
    }
}
