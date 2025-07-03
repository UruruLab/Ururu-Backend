package com.ururulab.ururu.payment.domain.repository;

import com.ururulab.ururu.payment.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문 ID로 결제 정보 조회
     * POST /payments/request에서 중복 결제 방지용
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * 결제 키로 결제 정보 조회 (연관 엔티티 포함)
     * 웹훅 처리 및 결제 승인 시 사용
     */
    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.order o " +
            "LEFT JOIN FETCH p.member m " +
            "WHERE p.paymentKey = :paymentKey")
    Optional<Payment> findByPaymentKeyWithDetails(@Param("paymentKey") String paymentKey);

    /**
     * 회원과 주문으로 결제 정보 조회
     * 결제 요청 시 기존 결제 검증용
     */
    @Query("SELECT p FROM Payment p " +
            "WHERE p.member.id = :memberId AND p.order.id = :orderId")
    Optional<Payment> findByMemberIdAndOrderId(@Param("memberId") Long memberId, @Param("orderId") String orderId);
}