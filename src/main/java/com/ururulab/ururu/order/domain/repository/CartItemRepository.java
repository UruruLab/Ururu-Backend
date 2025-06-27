package com.ururulab.ururu.order.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.order.domain.entity.Cart;
import com.ururulab.ururu.order.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndGroupBuyOption(Cart cart, GroupBuyOption groupBuyOption);

    /**
     * 장바구니 아이템 조회 (연관 엔티티 페치조인)
     * PUT /cart/items/{cartItemId}, DELETE /cart/items/{cartItemId}에서 사용
     */
    @Query("SELECT ci FROM CartItem ci " +
            "LEFT JOIN FETCH ci.groupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE ci.id = :cartItemId AND ci.cart.member.id = :memberId")
    Optional<CartItem> findByIdAndMemberIdWithDetails(
            @Param("cartItemId") Long cartItemId,
            @Param("memberId") Long memberId
    );
}