package com.ururulab.ururu.order.domain.repository;

import com.ururulab.ururu.order.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 특정 회원의 특정 공구 옵션 장바구니 수량 조회 (개인 구매 제한 검증용)
     * 장바구니 추가/수정 시 사용 - 기존 장바구니 수량 확인
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
}