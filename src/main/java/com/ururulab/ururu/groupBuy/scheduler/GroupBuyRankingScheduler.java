package com.ururulab.ururu.groupBuy.scheduler;

import com.ururulab.ururu.groupBuy.service.GroupBuyRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuyRankingScheduler {
    private final GroupBuyRankingService rankingService;

    @Scheduled(fixedRate = 180000) // 15분
    public void syncOrderCounts() {
        log.info("Starting scheduled Redis synchronization...");

        try {
            rankingService.syncOrderCountsFromDB();
            log.info("Scheduled Redis synchronization completed successfully");
        } catch (Exception e) {
            log.error("Failed to execute scheduled Redis synchronization", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void fullSyncOrderCounts() {
        log.info("Starting daily full Redis synchronization...");
        rankingService.syncOrderCountsFromDB();
    }
}
