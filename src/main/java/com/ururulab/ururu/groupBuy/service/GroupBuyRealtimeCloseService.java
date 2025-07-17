package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyStatisticsRepository;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ururulab.ururu.global.exception.error.ErrorCode.GROUPBUY_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyRealtimeCloseService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyStatisticsRepository groupBuyStatisticsRepository;
    private final GroupBuyStatisticsCalculatorService statisticsCalculatorService;
    private final GroupBuyPriceService groupBuyPriceService;
    private final GroupBuyOptionRepository groupBuyOptionRepository;

    /**
     * initialStock 기반 재고 소진 체크
     * 재고 소진으로 인한 즉시 공동구매 종료
     * 주문 완료 이벤트 리스너에서 호출
     *
     * @param groupBuyId 확인할 공동구매 ID
     */
    @Transactional
    public void checkAndCloseIfStockDepleted(Long groupBuyId) {
        log.debug("재고 소진 체크 시작 - groupBuyId: {}", groupBuyId);

        // 공동구매 조회 및 상태 확인
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

        // OPEN 상태가 아니면 처리하지 않음
        if (groupBuy.getStatus() != GroupBuyStatus.OPEN) {
            log.debug("공동구매가 OPEN 상태가 아님 - groupBuyId: {}, status: {}",
                    groupBuyId, groupBuy.getStatus());
            return;
        }

        boolean isStockDepleted = groupBuyOptionRepository.isAllStockDepleted(groupBuyId);

        if (!isStockDepleted) {
            log.debug("재고 여전히 남아있음 - groupBuyId: {}", groupBuyId);
            return;
        }

        // 재고 소진으로 조기 종료 처리
        log.info("재고 완전 소진으로 공동구매 즉시 종료 - groupBuyId: {}", groupBuyId);

        try {
            closeGroupBuyImmediately(groupBuy);

            // 재고 소진으로 종료 완료 (이벤트 발행 제거)
            log.info("재고 소진으로 공동구매 즉시 종료 완료 - groupBuyId: {}", groupBuyId);

        } catch (Exception e) {
            log.error("공동구매 즉시 종료 실패 - groupBuyId: {}", groupBuyId, e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 공동구매 즉시 종료 처리
     * @param groupBuy
     */
    private void closeGroupBuyImmediately(GroupBuy groupBuy) {
        log.info("공동구매 즉시 종료 처리 시작 - groupBuyId: {}", groupBuy.getId());

        // 통계 계산 (내부적으로 initialStock 기반으로 처리될 예정)
        GroupBuyStatistics statistics = statisticsCalculatorService.calculateSingleStatistics(groupBuy);

        // 상태를 CLOSED로 변경
        groupBuy.updateStatus(GroupBuyStatus.CLOSED);

        // 연관된 상품을 INACTIVE로 변경
        Product product = groupBuy.getProduct();
        if (product.getStatus() == Status.ACTIVE) {
            product.updateStatus(Status.INACTIVE);
            log.info("Product {} deactivated after GroupBuy deletion", product.getId());
        }

        // 최종 할인율이 있으면 최종 판매가 업데이트
        if (statistics.getFinalDiscountRate() > 0) {
            groupBuyPriceService.updateFinalSalePrices(
                    groupBuy,
                    statistics.getFinalDiscountRate()
            );
        }

        // 4. 변경사항 저장
        groupBuyRepository.save(groupBuy);
        groupBuyStatisticsRepository.save(statistics);

        log.info("공동구매 즉시 종료 완료 - groupBuyId: {}, 상태: {}, 참여자: {}, 수량: {}, 할인율: {}%",
                groupBuy.getId(), statistics.getFinalStatus(), statistics.getTotalParticipants(),
                statistics.getTotalQuantity(), statistics.getFinalDiscountRate());
    }
}
