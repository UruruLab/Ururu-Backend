package com.ururulab.ururu.groupBuy.scheduler;

import com.ururulab.ururu.groupBuy.service.GroupBuyBatchCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuyBatchScheduler {

    private final GroupBuyBatchCloseService batchCloseService;

    /**
     * 매일 자정(00:00)에 만료된 공동구매 배치 종료
     */
    @Scheduled(cron = "0 0 0 * * *",  zone = "Asia/Seoul")
    public void closeExpiredGroupBuysBatch() {
        log.info("Starting daily batch process for expired group buys...");

        try {
            batchCloseService.closeExpiredGroupBuys();
            log.info("Daily batch process completed successfully");

        } catch (Exception e) {
            log.error("Failed to execute daily batch process for group buy closure", e);
            // 배치 실패 시 알림 등 추가 처리 가능
        }
    }

    /**
     * 시간별 헬스체크용
     * 매 시간마다 급하게 처리해야 할 만료된 공동구매가 있는지 확인
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul") // 매시간 정각
    public void hourlyHealthCheck() {
        log.debug("Running hourly health check for group buy expiration...");

        try {
            // 급하게 처리해야 할 케이스가 있다면 여기서 처리
            // 예: 자정 배치에서 누락된 케이스 등
            batchCloseService.closeExpiredGroupBuys();

        } catch (Exception e) {
            log.warn("Hourly health check encountered an issue", e);
            // 헬스체크 실패는 심각하지 않으므로 경고 로그만
        }
    }
}
