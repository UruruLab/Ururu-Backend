package com.ururulab.ururu.groupBuy.event;

import java.util.List;

/**
 * 배치로 여러 공동구매가 종료되었을 때 발생하는 이벤트
 * 자정 스케줄러에서 시간 만료로 공동구매들이 일괄 종료될 때 발행
 */
public record GroupBuysBatchClosedEvent(
        List<Long> groupBuyIds
) {
}
