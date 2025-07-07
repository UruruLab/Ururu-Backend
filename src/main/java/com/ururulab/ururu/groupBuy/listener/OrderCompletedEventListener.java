package com.ururulab.ururu.groupBuy.listener;

import com.ururulab.ururu.groupBuy.event.OrderCompletedEvent;
import com.ururulab.ururu.groupBuy.service.GroupBuyRealtimeCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedEventListener {

    private final GroupBuyRealtimeCloseService realtimeCloseService;

    /**
     * 주문 완료 시 재고 소진 체크
     * 비동기로 처리하여 주문 응답 속도에 영향주지 않음
     */
    @EventListener
    @Async("stockCheckExecutor") // 별도 스레드풀에서 실행
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.debug("Processing order completed event for {} items", event.orderItems().size());

        try {
            // 해당 주문의 모든 공동구매 ID 추출 (중복 제거)
            Set<Long> groupBuyIds = event.orderItems().stream()
                    .map(OrderCompletedEvent.OrderItemInfo::groupBuyId)
                    .collect(Collectors.toSet());

            log.debug("Checking stock depletion for {} unique group buys", groupBuyIds.size());

            // 각 공동구매에 대해 재고 소진 체크
            for (Long groupBuyId : groupBuyIds) {
                try {
                    realtimeCloseService.checkAndCloseIfStockDepleted(groupBuyId);

                } catch (Exception e) {
                    log.error("Failed to check stock depletion for group buy: {}", groupBuyId, e);
                    // 개별 체크 실패가 다른 공동구매 체크에 영향주지 않도록 계속 진행
                }
            }

            log.debug("Completed stock depletion check for {} group buys", groupBuyIds.size());

        } catch (Exception e) {
            log.error("Failed to process order completed event", e);
            // 전체 처리 실패해도 주문 자체에는 영향 없음
        }
    }
}
