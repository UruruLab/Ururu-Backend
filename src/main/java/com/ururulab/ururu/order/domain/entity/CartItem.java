package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.order.domain.policy.CartItemPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "cart_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupbuy_option_id", nullable = false)
    private GroupBuyOption groupBuyOption;

    @Column(nullable = false)
    private int quantity;

    public static CartItem create(GroupBuyOption groupBuyOption, int quantity) {
        if (groupBuyOption == null) {
            throw new IllegalArgumentException(CartItemPolicy.GROUPBUY_OPTION_REQUIRED);
        }
        if (quantity < CartItemPolicy.MIN_QUANTITY) {
            throw new IllegalArgumentException(CartItemPolicy.QUANTITY_MIN);
        }

        CartItem cartItem = new CartItem();
        cartItem.groupBuyOption = groupBuyOption;
        cartItem.quantity = quantity;
        return cartItem;
    }

    public void assignCart(Cart cart) {
        this.cart = cart;
    }

    public void updateQuantity(int quantity) {
        if (quantity < CartItemPolicy.MIN_QUANTITY) {
            throw new IllegalArgumentException(CartItemPolicy.QUANTITY_MIN);
        }
        this.quantity = quantity;
    }

    public void increaseQuantity(int amount) {
        if (amount < CartItemPolicy.MIN_QUANTITY) {
            throw new IllegalArgumentException(CartItemPolicy.INCREASE_AMOUNT_MIN);
        }
        this.quantity += amount;
    }

    public void decreaseQuantity(int amount) {
        if (amount < CartItemPolicy.MIN_QUANTITY) {
            throw new IllegalArgumentException(CartItemPolicy.DECREASE_AMOUNT_MIN);
        }
        if (this.quantity - amount < 0) {
            throw new IllegalArgumentException(CartItemPolicy.QUANTITY_MIN);
        }
        this.quantity -= amount;
    }
}