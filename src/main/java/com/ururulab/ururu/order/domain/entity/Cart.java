package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.order.domain.policy.CartPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    public static Cart create(Member member) {
        if (member == null) {
            throw new IllegalArgumentException(CartPolicy.MEMBER_REQUIRED);
        }

        Cart cart = new Cart();
        cart.member = member;
        return cart;
    }

    public void addItem(CartItem cartItem) {
        if (cartItem == null) {
            throw new IllegalArgumentException(CartPolicy.CART_ITEM_REQUIRED);
        }

        cartItems.add(cartItem);
        cartItem.assignCart(this);
    }

    public void removeItem(CartItem cartItem) {
        if (cartItem == null) {
            throw new IllegalArgumentException(CartPolicy.CART_ITEM_REQUIRED);
        }
        cartItems.remove(cartItem);
    }

    public void removeItem(Long cartItemId) {
        if (cartItemId == null) {
            throw new IllegalArgumentException(CartPolicy.CART_ITEM_ID_REQUIRED);
        }
        cartItems.removeIf(item -> item.getId() != null && item.getId().equals(cartItemId));
    }

    public void clearItems() {
        cartItems.clear();
    }

    public int getTotalItemCount() {
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public boolean isEmpty() {
        return cartItems.isEmpty();
    }
}