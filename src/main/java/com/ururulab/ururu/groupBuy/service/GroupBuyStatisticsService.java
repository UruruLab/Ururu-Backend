package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyStatisticsRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyStatisticsDetailResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyStatisticsResponse;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GroupBuyStatisticsService {
    private final GroupBuyStatisticsRepository statisticsRepository;
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final SellerRepository sellerRepository;

    /**
     * initialStock 기반 공동구매 통계 상세 조회
     */
    public GroupBuyStatisticsDetailResponse getGroupBuyStatisticsDetail(Long groupBuyId, Long sellerId) {
        log.debug("Fetching group buy statistics detail - groupBuyId: {}, sellerId: {}", groupBuyId, sellerId);

        // 1. 공동구매 조회 및 본인 소유 확인
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND));

        if (!groupBuy.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ACCESS_DENIED);
        }

        // 2. 통계 데이터 조회
        GroupBuyStatistics statistics = statisticsRepository.findByGroupBuyId(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_STATISTICS_NOT_FOUND));

        // initialStock 기반 옵션별 판매량 조회
        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);

        // 4. 옵션별 판매량 정보 생성 (initialStock - stock 사용)
        List<GroupBuyStatisticsDetailResponse.GroupBuyOptionOrderInfo> optionOrderInfos = options.stream()
                .map(option -> {
                    String optionName = option.getProductOption().getName();
                    Integer soldQuantity = option.getSoldQuantity(); // initialStock - stock

                    log.debug("Option {} sold quantity: {}", option.getId(), soldQuantity);

                    return new GroupBuyStatisticsDetailResponse.GroupBuyOptionOrderInfo(
                            option.getId(),
                            optionName,
                            soldQuantity
                    );
                })
                .toList();

        log.info("Successfully fetched statistics for group buy: {} with {} options",
                groupBuyId, optionOrderInfos.size());

        // 5. 응답 생성
        return GroupBuyStatisticsDetailResponse.from(statistics, optionOrderInfos);
    }

    /**
     * 판매자의 모든 그룹 구매 통계 조회
     */
    public List<GroupBuyStatisticsResponse> getGroupBuyStatisticsBySeller(Long sellerId) {
        log.debug("Fetching group buy statistics for seller: {}", sellerId);

        if (!sellerRepository.existsById(sellerId)) {
            throw new BusinessException(SELLER_NOT_FOUND);
        }

        List<GroupBuyStatistics> statisticsList = statisticsRepository.findByGroupBuy_SellerId(sellerId);

        if (statisticsList.isEmpty()) {
            throw new BusinessException(GROUPBUY_STATISTICS_NOT_FOUND);
        }

        List<GroupBuyStatisticsResponse> responses = statisticsList
                .stream()
                .map(GroupBuyStatisticsResponse::from)
                .toList();

        log.info("Successfully fetched {} statistics for seller: {}", responses.size(), sellerId);

        return responses;
    }
}
