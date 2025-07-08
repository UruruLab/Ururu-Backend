package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyStatisticsRepository;
import com.ururulab.ururu.groupBuy.event.GroupBuysBatchClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupBuyBatchCloseService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyStatisticsRepository groupBuyStatisticsRepository;
    private final GroupBuyStatisticsCalculatorService statisticsCalculatorService;
    private final GroupBuyPriceService groupBuyPriceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 만료된 공동구매들을 배치로 종료 처리
     * 매일 자정에 실행되는 스케줄러에서 호출
     */
    public void closeExpiredGroupBuys() {
        log.info("Starting batch closure process for expired group buys...");

        Instant currentTime = Instant.now();

        // 만료된 공동구매들 조회 (필요한 연관 엔티티 한번에 페치)
        List<GroupBuy> expiredGroupBuys = groupBuyRepository.findExpiredGroupBuys(currentTime);

        if (expiredGroupBuys.isEmpty()) {
            log.info("No expired group buys found at {}", currentTime);
            return;
        }

        log.info("Found {} expired group buys to close", expiredGroupBuys.size());

        try {
            // 배치로 통계 계산
            List<GroupBuyStatistics> statisticsList = statisticsCalculatorService
                    .calculateBatchStatistics(expiredGroupBuys);

            updateGroupBuyStatusesAndPrices(expiredGroupBuys, statisticsList);

            // 먼저 상태를 DB에 반영
            groupBuyRepository.saveAll(expiredGroupBuys);

            // 통계 일괄 저장
            groupBuyStatisticsRepository.saveAll(statisticsList);

            // 성공적으로 처리된 공동구매 ID 목록
            List<Long> closedGroupBuyIds = expiredGroupBuys.stream()
                    .map(GroupBuy::getId)
                    .toList();

            log.info("Successfully closed {} group buys: {}",
                    closedGroupBuyIds.size(), closedGroupBuyIds);

            // 배치 종료 이벤트 발행
            eventPublisher.publishEvent(new GroupBuysBatchClosedEvent(closedGroupBuyIds));

        } catch (Exception e) {
            log.error("Failed to process batch closure for expired group buys", e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 공동구매 상태 업데이트 및 최종 가격 적용
     * @param groupBuys
     * @param statisticsList
     */
    private void updateGroupBuyStatusesAndPrices(List<GroupBuy> groupBuys,
                                                 List<GroupBuyStatistics> statisticsList) {

        for (int i = 0; i < groupBuys.size(); i++) {
            GroupBuy groupBuy = groupBuys.get(i);
            GroupBuyStatistics statistics = statisticsList.get(i);

            try {
                // 상태를 CLOSED로 변경
                groupBuy.updateStatus(GroupBuyStatus.CLOSED);

                // 최종 할인율이 있으면 최종 판매가 업데이트
                if (statistics.getFinalDiscountRate() > 0) {
                    groupBuyPriceService.updateFinalSalePrices(
                            groupBuy,
                            statistics.getFinalDiscountRate()
                    );
                }

                log.debug("Updated group buy {}: status=CLOSED, discount={}%, status={}",
                        groupBuy.getId(), statistics.getFinalDiscountRate(),
                        statistics.getFinalStatus());

            } catch (Exception e) {
                log.error("Failed to update group buy {}", groupBuy.getId(), e);
                // 개별 실패는 전체 배치에 영향주지 않음 (로그만 남기고 계속 진행)
            }
        }

        // 변경된 GroupBuy들을 일괄 저장
        groupBuyRepository.saveAll(groupBuys);
    }
}
