package com.ururulab.ururu.order.domain.repository;

import com.ururulab.ururu.order.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 특정 회원의 특정 공구 옵션 장바구니 수량 조회 (개인 구매 제한 검증용)
     * 장바구니 추가/수정 시 사용 - 기존 장바구니 수량 확인 (PENDING: 새 주문 시 자동 취소되므로 제외)
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.member.id = :memberId " +
            "AND oi.groupBuyOption.id = :groupBuyOptionId " +
            "AND o.status = 'ORDERED'")
    Integer getTotalOrderedQuantityByMemberAndOption(
            @Param("memberId") Long memberId,
            @Param("groupBuyOptionId") Long groupBuyOptionId
    );

    /**
     * 특정 공동구매의 유효 주문 수량 조회
     * - 공동구매 종료(CLOSED) 시, 최종 할인율 적용을 위한 기준 수량 계산에 사용
     * - 'ORDERED' 상태의 주문만 포함하며, 취소된 주문은 제외
     *
     * 예시:
     * 할인 조건:
     *   - 10개 이상 주문 시 10% 할인
     *   - 30개 이상 주문 시 20% 할인
     * → 실제 주문 수량이 35개인 경우, 20% 할인율 적용
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE oi.groupBuyOption.groupBuy.id = :groupBuyId " +
            "AND o.status = 'ORDERED'")
    Integer getTotalQuantityByGroupBuyId(@Param("groupBuyId") Long groupBuyId);

    /**
     * 주문 ID로 환불 가능한 아이템 조회
     * 아직 환불되지 않은 주문 아이템들을 반환
     */
    @Query("SELECT oi FROM OrderItem oi " +
            "WHERE oi.order.id = :orderId " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM RefundItem ri " +
            "  JOIN ri.refund r " +
            "  WHERE ri.orderItem.id = oi.id " +
            "  AND r.status IN ('APPROVED', 'COMPLETED', 'FAILED')" +
            ")")
    List<OrderItem> findRefundableItemsByOrderId(@Param("orderId") String orderId);
}