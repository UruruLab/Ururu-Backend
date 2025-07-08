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
     * 진행중 공구의 주문 목록 조회 (연관 엔티티 페치조인)
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE o.member.id = :memberId " +
            "AND o.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
            "AND gb.status = 'OPEN' " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM RefundItem ri " +
            "  JOIN ri.refund r " +
            "  WHERE ri.orderItem.id = oi.id " +
            "  AND r.status IN ('APPROVED', 'COMPLETED')" +
            ") " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findInProgressOrdersWithDetails(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    /**
     * 확정된 공구의 주문 목록 조회 (연관 엔티티 페치조인)
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE o.member.id = :memberId " +
            "AND o.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
            "AND gb.status = 'CLOSED' " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM RefundItem ri " +
            "  JOIN ri.refund r " +
            "  WHERE ri.orderItem.id = oi.id " +
            "  AND r.status IN ('APPROVED', 'COMPLETED')" +
            ") " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findConfirmedOrdersWithDetails(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    /**
     * 환불 대기중 주문 목록 조회 (연관 엔티티 페치조인)
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
            "  SELECT 1 FROM RefundItem ri " +
            "  JOIN ri.refund r " +
            "  WHERE ri.orderItem.id = oi.id " +
            "  AND r.status = 'INITIATED'" +
            ") " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findRefundPendingOrdersWithDetails(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    /**
     * 전체 주문 목록 조회 (연관 엔티티 페치조인)
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE o.member.id = :memberId " +
            "AND o.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM RefundItem ri " +
            "  JOIN ri.refund r " +
            "  WHERE ri.orderItem.id = oi.id " +
            "  AND r.status IN ('APPROVED', 'COMPLETED')" +
            ") " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findAllOrdersWithDetails(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    /**
     * 1. inProgress: OPEN 상태 + 환불 안된 상품들
     */
    @Query("SELECT COUNT(DISTINCT o.id) " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "JOIN oi.groupBuyOption gbo " +
            "JOIN gbo.groupBuy gb " +
            "WHERE o.member.id = :memberId " +
            "AND o.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
            "AND gb.status = 'OPEN' " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM RefundItem ri " +
            "  JOIN ri.refund r " +
            "  WHERE ri.orderItem.id = oi.id" +
            ")")
    Long countInProgressOrders(@Param("memberId") Long memberId);

    /**
     * 2. confirmed: CLOSE 상태 + 환불 안된 상품들
     */
    @Query("SELECT COUNT(DISTINCT o.id) " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "JOIN oi.groupBuyOption gbo " +
            "JOIN gbo.groupBuy gb " +
            "WHERE o.member.id = :memberId " +
            "AND o.status IN ('ORDERED', 'PARTIAL_REFUNDED') " +
            "AND gb.status != 'OPEN' " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM RefundItem ri " +
            "  JOIN ri.refund r " +
            "  WHERE ri.orderItem.id = oi.id" +
            ")")
    Long countConfirmedOrders(@Param("memberId") Long memberId);

    /**
     * 3. refundPending: 환불 INITIATED 상태
     */
    @Query("SELECT COUNT(r.id) " +
            "FROM Refund r " +
            "JOIN r.payment p " +
            "WHERE p.member.id = :memberId " +
            "AND r.status = 'INITIATED'")
    Long countRefundPendingOrders(@Param("memberId") Long memberId);
}