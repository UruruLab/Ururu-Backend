package com.ururulab.ururu.order.domain.repository;

import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
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
}