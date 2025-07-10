package com.ururulab.ururu.groupBuy.event;

import java.util.Set;

public record StockDepletedEvent(
        Set<Long> groupBuyIds
) {
    /**
     * 재고가 소진된 공동구매 ID Set으로 이벤트 생성
     * @param groupBuyIds 재고가 0이 된 공동구매 ID들
     * @return StockDepletedEvent
     */
    public static StockDepletedEvent of(Set<Long> groupBuyIds) {
        return new StockDepletedEvent(groupBuyIds);
    }

    /**
     * 이벤트가 처리할 공동구매가 있는지 확인
     * @return 처리할 공동구매가 있으면 true
     */
    public boolean hasGroupBuysToProcess() {
        return groupBuyIds != null && !groupBuyIds.isEmpty();
    }

    /**
     * 처리 대상 공동구매 개수
     * @return 공동구매 개수
     */
    public int getGroupBuyCount() {
        return groupBuyIds != null ? groupBuyIds.size() : 0;
    }
}
