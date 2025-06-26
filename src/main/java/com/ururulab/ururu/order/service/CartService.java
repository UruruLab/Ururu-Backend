package com.ururulab.ururu.order.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.dto.request.CartItemAddRequest;
import com.ururulab.ururu.order.domain.dto.request.CartItemQuantityChangeRequest;
import com.ururulab.ururu.order.domain.dto.response.CartItemAddResponse;
import com.ururulab.ururu.order.domain.dto.response.CartItemQuantityChangeResponse;
import com.ururulab.ururu.order.domain.dto.response.CartItemResponse;
import com.ururulab.ururu.order.domain.dto.response.CartResponse;
import com.ururulab.ururu.order.domain.entity.Cart;
import com.ururulab.ururu.order.domain.entity.CartItem;
import com.ururulab.ururu.order.domain.repository.CartRepository;
import com.ururulab.ururu.order.domain.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final MemberRepository memberRepository;

    /**
     * 장바구니에 아이템을 추가
     *
     * @param memberId 회원 ID
     * @param request 장바구니 아이템 추가 요청 DTO
     */
    @Transactional
    public CartItemAddResponse addCartItem(Long memberId, CartItemAddRequest request) {
        log.debug("장바구니 아이템 추가 - 회원ID: {}, 옵션ID: {}, 수량: {}",
                memberId, request.groupbuyOptionId(), request.quantity());

        GroupBuyOption groupBuyOption = groupBuyOptionRepository
                .findByIdWithDetails(request.groupbuyOptionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공구 옵션입니다."));

        ZonedDateTime endsAt = toZonedDateTime(groupBuyOption.getGroupBuy().getEndsAt());
        if (endsAt.isBefore(ZonedDateTime.now(ZoneId.of("Asia/Seoul")))) {
            throw new IllegalStateException("종료된 공구입니다.");
        }
        Cart cart = getOrCreateCart(memberId);

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartAndGroupBuyOption(cart, groupBuyOption);

        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            cartItem.increaseQuantity(request.quantity());
            log.debug("기존 장바구니 아이템 수량 증가 - 아이템ID: {}, 새 수량: {}",
                    cartItem.getId(), cartItem.getQuantity());
        } else {
            cartItem = CartItem.create(groupBuyOption, request.quantity());
            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);
            log.debug("새 장바구니 아이템 추가 - 옵션ID: {}, 수량: {}",
                    groupBuyOption.getId(), request.quantity());
        }

        return new CartItemAddResponse(
                cartItem.getId(),
                cartItem.getQuantity()
        );
    }

    /**
     * 장바구니 조회
     * @param memberId 회원 ID
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long memberId) {
        log.debug("장바구니 조회 - 회원ID: {}", memberId);

        Optional<Cart> cartOpt = cartRepository.findByMemberIdWithCartItems(memberId);

        if (cartOpt.isEmpty() || cartOpt.get().isEmpty()) {
            return new CartResponse(List.of());
        }

        Cart cart = cartOpt.get();

        List<CartItemResponse> cartItemResponses = cart.getCartItems().stream()
                .filter(this::isNotExpired)
                .map(this::toCartItemResponse)
                .toList();

        return new CartResponse(cartItemResponses);
    }

    /**
     * 장바구니 아이템 수량 변경
     * @param memberId 회원 ID
     * @param cartItemId 변경 대상 장바구니 아이템 ID
     * @param request 수량 변경 요청 DTO
     */
    @Transactional
    public CartItemQuantityChangeResponse updateCartItemQuantity(Long memberId, Long cartItemId, CartItemQuantityChangeRequest request) {
        log.debug("장바구니 아이템 수량 변경 - 회원ID: {}, 아이템ID: {}, 변화량: {}",
                memberId, cartItemId, request.quantityChange());

        CartItem cartItem = cartItemRepository
                .findByIdAndMemberIdWithDetails(cartItemId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));

        if (request.quantityChange() > 0) {
            cartItem.increaseQuantity(request.quantityChange());
        } else {
            cartItem.decreaseQuantity(Math.abs(request.quantityChange()));
        }

        log.debug("장바구니 아이템 수량 변경 완료 - 아이템ID: {}, 새 수량: {}", cartItemId, cartItem.getQuantity());
        return new CartItemQuantityChangeResponse(cartItemId, cartItem.getQuantity());
    }

    /**
     * 장바구니 아이템 삭제
     * @param memberId 회원 ID
     * @param cartItemId 삭제할 장바구니 아이템 ID
     */
    @Transactional
    public void removeCartItem(Long memberId, Long cartItemId) {
        log.debug("장바구니 아이템 삭제 - 회원ID: {}, 아이템ID: {}", memberId, cartItemId);

        CartItem cartItem = cartItemRepository
                .findByIdAndMemberIdWithDetails(cartItemId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다."));

        Cart cart = cartItem.getCart();
        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        log.debug("장바구니 아이템 삭제 완료 - 아이템ID: {}", cartItemId);
    }

    private Cart getOrCreateCart(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
                    Cart newCart = Cart.create(member);
                    return cartRepository.save(newCart);
                });
    }

    private boolean isNotExpired(CartItem cartItem) {
        ZonedDateTime endsAt = toZonedDateTime(cartItem.getGroupBuyOption().getGroupBuy().getEndsAt());
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        return endsAt.isAfter(now);
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        GroupBuyOption option = cartItem.getGroupBuyOption();

        return new CartItemResponse(
                cartItem.getId(),
                option.getId(),
                cartItem.getQuantity(),
                option.getGroupBuy().getProduct().getName(),
                option.getProductOption().getName(),
                option.getProductOption().getImageUrl(),
                option.getSalePrice(),
                toZonedDateTime(option.getGroupBuy().getEndsAt())
        );
    }

    /**
     * LocalDateTime 등 다양한 시간 객체를 ZonedDateTime으로 변환
     * LocalDateTime이면 Asia/Seoul 기준으로, Zoned/OffsetDateTime이면 그대로
     * 시간 객체 타입이 ZonedDateTime으로 확정 시 메서드 삭제 예정
     */
    private ZonedDateTime toZonedDateTime(Object time) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");

        if (time instanceof ZonedDateTime zdt) {
            return zdt;
        } else if (time instanceof OffsetDateTime odt) {
            return odt.toZonedDateTime();
        } else if (time instanceof LocalDateTime ldt) {
            return ldt.atZone(zoneId);
        } else if (time instanceof Instant instant) {
            return instant.atZone(zoneId);
        } else {
            throw new IllegalArgumentException("지원하지 않는 시간 타입입니다: " +
                    (time != null ? time.getClass().getName() : "null"));
        }
    }
}
