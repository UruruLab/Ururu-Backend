package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.FinalStatus;

import java.time.Instant;

public record GroupBuyStatisticsResponse(
        Long groupBuyId,
        String groupBuyTitle,
        Integer totalParticipants,
        Integer totalQuantity,
        Integer finalDiscountRate,
        FinalStatus finalStatus,
        Instant confirmedAt,
        Boolean isSuccessful
) {
    public static GroupBuyStatisticsResponse from(GroupBuyStatistics statistics) {
        return new GroupBuyStatisticsResponse(
                statistics.getGroupBuy().getId(),
                statistics.getGroupBuy().getTitle(),
                statistics.getTotalParticipants(),
                statistics.getTotalQuantity(),
                statistics.getFinalDiscountRate(),
                statistics.getFinalStatus(),
                statistics.getConfirmedAt(),
                statistics.getFinalStatus() == FinalStatus.SUCCESS
        );
    }
}
