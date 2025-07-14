package com.ururulab.ururu.payment.domain.repository;

import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    /**
     * 환불 ID로 상세 정보 조회
     * 환불 처리 시 연관 엔티티들 함께 조회
     */
    @Query("SELECT r FROM Refund r " +
            "LEFT JOIN FETCH r.payment p " +
            "LEFT JOIN FETCH p.order o " +
            "LEFT JOIN FETCH p.member m " +
            "LEFT JOIN FETCH r.refundItems ri " +
            "LEFT JOIN FETCH ri.orderItem oi " +
            "LEFT JOIN FETCH oi.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product prod " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE r.id = :refundId")
    Optional<Refund> findByIdWithDetails(@Param("refundId") String refundId);

    /**
     * 주문에 대한 수동환불 진행중 여부 확인
     * 수동환불은 Order 단위 전체 처리이므로 하나라도 진행중이면 중복 방지
     *
     * @param orderId 주문 ID
     * @param memberId 회원 ID
     * @return 진행중인 수동환불 정보
     */
    @Query("SELECT r FROM Refund r " +
            "JOIN r.payment p " +
            "WHERE p.order.id = :orderId " +
            "AND p.member.id = :memberId " +
            "AND r.status = 'INITIATED' " +
            "AND r.type != 'GROUPBUY_FAILED'")
    Optional<Refund> findActiveManualRefundByOrderAndMember(
            @Param("orderId") String orderId,
            @Param("memberId") Long memberId
    );

    /**
     * 판매자의 환불 요청 목록 조회
     * 판매자별 환불 관리용
     *
     * @param sellerId 판매자 ID
     * @param status 환불 상태
     * @return 환불 목록
     */
    @Query("SELECT r FROM Refund r " +
            "JOIN r.payment p " +
            "JOIN p.order o " +
            "JOIN o.orderItems oi " +
            "JOIN oi.groupBuyOption gbo " +
            "JOIN gbo.groupBuy gb " +
            "WHERE gb.seller.id = :sellerId " +
            "AND r.status = :status " +
            "ORDER BY r.createdAt DESC")
    List<Refund> findBySellerId(
            @Param("sellerId") Long sellerId,
            @Param("status") RefundStatus status
    );

    /**
     * 주문 ID로 환불 목록 조회
     * 동일 주문에 대한 기존 환불 내역 확인용
     */
    @Query("SELECT r FROM Refund r " +
            "JOIN r.payment p " +
            "WHERE p.order.id = :orderId")
    List<Refund> findByOrderId(@Param("orderId") String orderId);

    /**
     * 특정 주문에 대한 현재 진행중인 환불 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 진행중인 환불 정보 (INITIATED 상태), 없으면 Optional.empty()
     */
    @Query("SELECT r FROM Refund r " +
            "WHERE r.payment.order.id = :orderId " +
            "AND r.status = 'INITIATED'")
    Optional<Refund> findActiveRefundByOrderId(@Param("orderId") String orderId);
}
