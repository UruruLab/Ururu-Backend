package com.ururulab.ururu.order.domain.repository;

import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.order.domain.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByMemberId(Long memberId);

    /**
     * 회원의 장바구니 조회 (연관 엔티티 페치조인)
     * GET /cart API에서 사용 - N+1 문제 방지
     * 조회 시점에서 만료된 공구 필터링 예정
     */
    @Query("SELECT c FROM Cart c " +
            "LEFT JOIN FETCH c.cartItems ci " +
            "LEFT JOIN FETCH ci.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE c.member.id = :memberId")
    Optional<Cart> findByMemberIdWithCartItems(@Param("memberId") Long memberId);
}
