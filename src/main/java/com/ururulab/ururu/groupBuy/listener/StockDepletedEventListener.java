package com.ururulab.ururu.groupBuy.listener;

import com.ururulab.ururu.groupBuy.event.StockDepletedEvent;
import com.ururulab.ururu.groupBuy.service.GroupBuyRealtimeCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDepletedEventListener {
    private final GroupBuyRealtimeCloseService realtimeCloseService;

    /**
     * 재고 소진 이벤트 처리
     * - 재고가 0이 된 공동구매가 있을 때만 실행
     *
     * @param event 재고 소진 이벤트
     */
    @EventListener
    @Async("stockCheckExecutor")
    public void handleStockDepleted(StockDepletedEvent event) {
        if (!event.hasGroupBuysToProcess()) {
            log.debug("재고 소진된 공동구매가 없어 처리 건너뜀");
            return;
        }

        log.info("재고 소진 이벤트 처리 시작 - 대상 공동구매: {}개", event.getGroupBuyCount());
        log.debug("재고 소진된 공동구매 ID들: {}", event.groupBuyIds());

        try {
            int successCount = 0;
            int failCount = 0;

            // 재고가 소진된 공동구매들만 종료 처리
            for (Long groupBuyId : event.groupBuyIds()) {
                try {
                    log.info("공동구매 {} 재고 소진으로 즉시 종료 처리 시작", groupBuyId);
                    realtimeCloseService.checkAndCloseIfStockDepleted(groupBuyId);
                    successCount++;
                    log.info("공동구매 {} 종료 처리 완료", groupBuyId);
                } catch (Exception e) {
                    failCount++;
                    log.error("공동구매 {} 자동 종료 실패", groupBuyId, e);
                    // 개별 공동구매 종료 실패해도 다른 공동구매 처리는 계속
                }
            }

            log.info("재고 소진 이벤트 처리 완료 - 성공: {}개, 실패: {}개", successCount, failCount);

        } catch (Exception e) {
            log.error("재고 소진 이벤트 전체 처리 실패", e);
        }
    }
}
