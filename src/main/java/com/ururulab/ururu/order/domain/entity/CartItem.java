package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "cart_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // TODO: GroupBuyOption 엔티티 완성 후 연관관계로 변경
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "groupbuy_option_id", nullable = false)
    // private GroupBuyOption groupBuyOption;

    @Column(name = "groupbuy_option_id", nullable = false)
    private Long groupBuyOptionId;

    @Column(nullable = false)
    private int quantity;

    public static CartItem create(Long groupBuyOptionId, int quantity) {
        if (groupBuyOptionId == null) {
            throw new IllegalArgumentException("공동구매 상품 옵션 ID는 필수입니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        CartItem cartItem = new CartItem();
        cartItem.groupBuyOptionId = groupBuyOptionId;
        cartItem.quantity = quantity;
        return cartItem;
    }

    public void assignCart(Cart cart) {
        this.cart = cart;
    }

    public void updateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("증가량은 1 이상이어야 합니다.");
        }
        this.quantity += amount;
    }

    public void decreaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("감소량은 1 이상이어야 합니다.");
        }
        if (this.quantity - amount < 0) {
            throw new IllegalArgumentException("수량은 0 미만이 될 수 없습니다.");
        }
        this.quantity -= amount;
    }
}