package com.ururulab.ururu.payment.domain.repository;

import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, String> {

    /**
     * 회원의 환불 목록 조회 (INITIATED 이후 상태만, 연관 엔티티 페치조인)
     */
    @Query("SELECT r FROM Refund r " +
            "LEFT JOIN FETCH r.refundItems ri " +
            "LEFT JOIN FETCH ri.orderItem oi " +
            "LEFT JOIN FETCH oi.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE r.payment.member.id = :memberId " +
            "AND r.status != 'INITIATED' " +
            "AND (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.createdAt DESC")
    Page<Refund> findProcessedRefundsByMemberId(
            @Param("memberId") Long memberId,
            @Param("status") RefundStatus status,
            Pageable pageable
    );
}
