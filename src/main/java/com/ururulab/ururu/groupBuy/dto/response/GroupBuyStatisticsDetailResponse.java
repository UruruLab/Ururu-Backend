package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.FinalStatus;

import java.time.Instant;
import java.util.List;

public record GroupBuyStatisticsDetailResponse(

        Long groupBuyId,
        String groupBuyTitle,
        Integer totalParticipants,
        Integer totalQuantity,
        Integer finalDiscountRate,
        FinalStatus finalStatus,
        Instant confirmedAt,
        Boolean isSuccessful,
        List<GroupBuyOptionOrderInfo> optionOrderInfos
) {
    public static GroupBuyStatisticsDetailResponse from(
            GroupBuyStatistics statistics,
            List<GroupBuyOptionOrderInfo> optionOrderInfos
    ) {
        return new GroupBuyStatisticsDetailResponse(
                statistics.getGroupBuy().getId(),
                statistics.getGroupBuy().getTitle(),
                statistics.getTotalParticipants(),
                statistics.getTotalQuantity(),
                statistics.getFinalDiscountRate(),
                statistics.getFinalStatus(),
                statistics.getConfirmedAt(),
                statistics.getFinalStatus() == FinalStatus.SUCCESS,
                optionOrderInfos
        );
    }

    public record GroupBuyOptionOrderInfo(
            Long optionId,
            String optionName,
            Integer orderCount
    ) {
    }
}
