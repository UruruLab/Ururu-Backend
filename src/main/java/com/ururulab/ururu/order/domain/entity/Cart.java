package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "cart")
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
            throw new IllegalArgumentException("회원 정보는 필수입니다.");
        }

        Cart cart = new Cart();
        cart.member = member;
        return cart;
    }

    public void addItem(CartItem cartItem) {
        if (cartItem == null) {
            throw new IllegalArgumentException("장바구니 아이템은 필수입니다.");
        }

        cartItems.add(cartItem);
        cartItem.assignCart(this);
    }

    public void removeItem(CartItem cartItem) {
        cartItems.remove(cartItem);
    }

    public void removeItem(Long cartItemId) {
        if (cartItemId == null) {
            throw new IllegalArgumentException("장바구니 아이템 ID는 필수입니다.");
        }
        cartItems.removeIf(item -> item.getId().equals(cartItemId));
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