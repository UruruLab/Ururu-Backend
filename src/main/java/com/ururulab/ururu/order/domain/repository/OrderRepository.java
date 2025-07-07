package com.ururulab.ururu.order.domain.repository;

import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * 특정 회원의 PENDING 상태 주문들 조회
     * 재주문 시 기존 주문 취소용
     */
    @Query("SELECT o FROM Order o WHERE o.member.id = :memberId AND o.status = :status")
    List<Order> findByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.member.id = :memberId AND o.status IN ('PENDING', 'ORDERED')")
    int countActiveOrdersByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 주문 목록 조회 - 환불되지 않은 OrderItem이 있는 주문만
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE o.member.id = :memberId " +
            "AND o.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
            "AND EXISTS (" +
            "  SELECT 1 FROM OrderItem oi2 " +
            "  WHERE oi2.order.id = o.id " +
            "  AND NOT EXISTS (" +
            "    SELECT 1 FROM RefundItem ri " +
            "    JOIN ri.refund r " +
            "    WHERE ri.orderItem.id = oi2.id " +
            "    AND r.status IN ('APPROVED', 'COMPLETED')" +
            "  )" +
            ") " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findMyOrdersWithDetails(
            @Param("memberId") Long memberId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    /**
     * 회원별 주문 상태 통계 조회 (수정된 버전)
     */
    @Query(value =
            "SELECT " +
                    "  (SELECT COUNT(DISTINCT o1.id) FROM orders o1 " +
                    "   JOIN order_items oi1 ON o1.id = oi1.order_id " +
                    "   JOIN groupbuy_options gbo1 ON oi1.groupbuy_option_id = gbo1.id " +
                    "   JOIN groupbuys gb1 ON gbo1.groupbuy_id = gb1.id " +
                    "   WHERE o1.member_id = :memberId " +
                    "   AND o1.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
                    "   AND gb1.status = 'OPEN' " +
                    "   AND NOT EXISTS (" +
                    "     SELECT 1 FROM refund_item ri1 " +
                    "     JOIN refund r1 ON ri1.refund_id = r1.id " +
                    "     WHERE ri1.order_item_id = oi1.id " +
                    "     AND r1.status IN ('APPROVED', 'COMPLETED')" +
                    "   )) as inProgress, " +
                    "  (SELECT COUNT(DISTINCT o2.id) FROM orders o2 " +
                    "   WHERE o2.member_id = :memberId " +
                    "   AND o2.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
                    "   AND EXISTS (" +
                    "     SELECT 1 FROM order_items oi2 " +
                    "     WHERE oi2.order_id = o2.id " +
                    "     AND NOT EXISTS (" +
                    "       SELECT 1 FROM refund_item ri2 " +
                    "       JOIN refund r2 ON ri2.refund_id = r2.id " +
                    "       WHERE ri2.order_item_id = oi2.id " +
                    "       AND r2.status IN ('APPROVED', 'COMPLETED')" +
                    "     )" +
                    "   ) " +
                    "   AND NOT EXISTS (" +
                    "     SELECT 1 FROM order_items oi3 " +
                    "     JOIN groupbuy_options gbo3 ON oi3.groupbuy_option_id = gbo3.id " +
                    "     JOIN groupbuys gb3 ON gbo3.groupbuy_id = gb3.id " +
                    "     WHERE oi3.order_id = o2.id " +
                    "     AND gb3.status = 'OPEN' " +
                    "     AND NOT EXISTS (" +
                    "       SELECT 1 FROM refund_item ri3 " +
                    "       JOIN refund r3 ON ri3.refund_id = r3.id " +
                    "       WHERE ri3.order_item_id = oi3.id " +
                    "       AND r3.status IN ('APPROVED', 'COMPLETED')" +
                    "     )" +
                    "   )) as confirmed, " +
                    "  (SELECT COUNT(r.id) FROM refund r " +
                    "   JOIN payment p ON r.payment_id = p.id " +
                    "   WHERE p.member_id = :memberId AND r.status = 'INITIATED') as refundPending",
            nativeQuery = true)
    OrderStatisticsProjection getOrderStatisticsByMemberId(@Param("memberId") Long memberId);

    interface OrderStatisticsProjection {
        Long getInProgress();
        Long getConfirmed();
        Long getRefundPending();
    }
}