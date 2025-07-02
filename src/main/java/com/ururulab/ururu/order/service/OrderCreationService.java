package com.ururulab.ururu.order.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.controller.dto.request.CartOrderCreateRequest;
import com.ururulab.ururu.order.controller.dto.request.GroupBuyOrderCreateRequest;
import com.ururulab.ururu.order.controller.dto.request.OrderItemRequest;
import com.ururulab.ururu.order.controller.dto.response.OrderCreateResponse;
import com.ururulab.ururu.order.controller.dto.response.OrderItemResponse;
import com.ururulab.ururu.order.domain.entity.CartItem;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.CartItemRepository;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final MemberRepository memberRepository;
    private final StockReservationService stockReservationService;

    /**
     * 공구 주문서 생성
     *
     * @param memberId 회원 ID
     * @param groupbuyId 공구 ID
     * @param request 주문 요청 정보
     * @return 생성된 주문서 정보
     * @throws BusinessException 회원/옵션 미존재, 재고 부족, 개인 제한 초과, 중복 요청 등
     */
    @Transactional
    public OrderCreateResponse createGroupBuyOrder(Long memberId, Long groupbuyId, GroupBuyOrderCreateRequest request) {
        log.debug("공구 주문서 생성 - 회원ID: {}, 공구ID: {}, 요청: {}", memberId, groupbuyId, request);

        acquireProcessingLock(memberId);

        try {
            cancelPendingOrders(memberId);

            OrderCreationContext context = prepareGroupBuyOrderContext(memberId, groupbuyId, request);
            validateOrderCreation(context);
            reserveStock(context);

            return createAndSaveOrder(context);

        } finally {
            releaseProcessingLockSafely(memberId);
        }
    }

    /**
     * 장바구니 주문서 생성
     *
     * @param memberId 회원 ID
     * @param request 장바구니 주문 요청 정보
     * @return 생성된 주문서 정보
     * @throws BusinessException 회원 미존재, 장바구니 아이템 없음, 재고 부족, 개인 제한 초과 등
     */
    @Transactional
    public OrderCreateResponse createCartOrder(Long memberId, CartOrderCreateRequest request) {
        log.debug("장바구니 주문서 생성 - 회원ID: {}, 요청: {}", memberId, request);

        acquireProcessingLock(memberId);

        try {
            cancelPendingOrders(memberId);

            OrderCreationContext context = prepareCartOrderContext(memberId, request);
            validateOrderCreation(context);
            reserveStock(context);

            return createAndSaveOrder(context);

        } finally {
            releaseProcessingLockSafely(memberId);
        }
    }

    /**
     * 처리 락 획득
     *
     * @param memberId 회원 ID
     * @throws BusinessException 이미 진행 중인 주문이 있거나 시스템 장애 시
     */
    private void acquireProcessingLock(Long memberId) {
        if (!stockReservationService.tryAcquireProcessingLock(memberId)) {
            throw new BusinessException(ErrorCode.ORDER_PROCESSING_IN_PROGRESS);
        }
    }

    /**
     * 처리 락을 안전하게 해제합니다.
     * Redis 연결 장애가 있어도 예외를 발생시키지 않습니다.
     *
     * @param memberId 회원 ID
     */
    private void releaseProcessingLockSafely(Long memberId) {
        try {
            stockReservationService.releaseProcessingLock(memberId);
        } catch (Exception e) {
            log.warn("락 해제 중 오류 발생 (무시됨): memberId={}", memberId, e);
            // Redis 장애 등으로 락 해제 실패해도 TTL에 의해 자동 해제되므로 무시
        }
    }

    /**
     * 기존 PENDING 주문들 취소
     *
     * @param memberId 회원 ID
     */
    private void cancelPendingOrders(Long memberId) {
        List<Order> pendingOrders = orderRepository.findByMemberIdAndStatus(memberId, OrderStatus.PENDING);

        if (!pendingOrders.isEmpty()) {
            for (Order order : pendingOrders) {
                order.changeStatus(OrderStatus.CANCELLED, "새로운 주문으로 인한 자동 취소");
            }
            stockReservationService.releaseAllUserReservations(memberId);
            log.debug("기존 PENDING 주문 취소 - 회원ID: {}, 취소된 주문 수: {}", memberId, pendingOrders.size());
        }
    }

    /**
     * 공구 주문 컨텍스트 준비
     *
     * @param memberId 회원 ID
     * @param groupbuyId 공구 ID
     * @param request 주문 요청 정보
     * @return 주문 생성에 필요한 컨텍스트
     * @throws BusinessException 회원 미존재, 옵션 미존재, 공구 불일치 시
     */
    private OrderCreationContext prepareGroupBuyOrderContext(Long memberId, Long groupbuyId, GroupBuyOrderCreateRequest request) {
        Member member = findMemberById(memberId);

        List<GroupBuyOption> groupBuyOptions = new ArrayList<>();
        GroupBuy groupBuy = null;

        for (OrderItemRequest orderItem : request.orderItems()) {
            GroupBuyOption option = findGroupBuyOptionById(orderItem.groupbuyOptionId());

            if (!option.getGroupBuy().getId().equals(groupbuyId)) {
                throw new BusinessException(ErrorCode.GROUPBUY_OPTION_MISMATCH, orderItem.groupbuyOptionId());
            }

            if (groupBuy == null) {
                groupBuy = option.getGroupBuy();
            }

            groupBuyOptions.add(option);
        }

        List<OrderItemRequest> orderItems = request.orderItems();
        return new OrderCreationContext(member, groupBuy, groupBuyOptions, orderItems);
    }

    /**
     * 장바구니 주문 컨텍스트 준비
     * 여러 공구의 상품을 함께 주문할 수 있습니다.
     *
     * @param memberId 회원 ID
     * @param request 장바구니 주문 요청 정보
     * @return 주문 생성에 필요한 컨텍스트
     * @throws BusinessException 회원 미존재, 장바구니 아이템 없음 시
     */
    private OrderCreationContext prepareCartOrderContext(Long memberId, CartOrderCreateRequest request) {
        Member member = findMemberById(memberId);

        List<CartItem> cartItems = cartItemRepository.findAllById(request.cartItemIds())
                .stream()
                .filter(item -> item.getCart().getMember().getId().equals(memberId))
                .toList();

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEMS_EMPTY);
        }

        // 여러 공구 허용 - MULTIPLE_GROUPBUY_NOT_ALLOWED 제약 제거됨
        // 첫 번째 아이템의 공구를 대표 공구로 사용 (Order 엔티티 호환성을 위한 임시 방편)
        GroupBuy groupBuy = cartItems.get(0).getGroupBuyOption().getGroupBuy();
        List<GroupBuyOption> groupBuyOptions = cartItems.stream()
                .map(CartItem::getGroupBuyOption)
                .toList();

        List<OrderItemRequest> orderItems = cartItems.stream()
                .map(item -> new OrderItemRequest(item.getGroupBuyOption().getId(), item.getQuantity()))
                .toList();

        return new OrderCreationContext(member, groupBuy, groupBuyOptions, orderItems);
    }

    /**
     * 주문 생성 검증
     * 공구 상태, 재고, 개인 구매 제한을 확인합니다.
     *
     * @param context 주문 생성 컨텍스트
     * @throws BusinessException 공구 종료, 재고 부족, 개인 제한 초과 시
     */
    private void validateOrderCreation(OrderCreationContext context) {
        validateGroupBuyStatus(context.getGroupBuy());

        for (int i = 0; i < context.getOrderItems().size(); i++) {
            OrderItemRequest orderItem = context.getOrderItems().get(i);
            GroupBuyOption option = context.getGroupBuyOptions().get(i);

            validateStock(option, orderItem.quantity());
            validatePersonalLimit(context.getMember().getId(), option, orderItem.quantity());
        }
    }

    /**
     * 재고 예약
     * Redis에 30분간 임시 재고를 예약합니다.
     *
     * @param context 주문 생성 컨텍스트
     */
    private void reserveStock(OrderCreationContext context) {
        for (int i = 0; i < context.getOrderItems().size(); i++) {
            OrderItemRequest orderItem = context.getOrderItems().get(i);
            GroupBuyOption option = context.getGroupBuyOptions().get(i);

            stockReservationService.reserveStock(option.getId(), context.getMember().getId(), orderItem.quantity());
        }
    }

    /**
     * 주문 생성 및 저장
     *
     * @param context 주문 생성 컨텍스트
     * @return 생성된 주문서 정보
     */
    private OrderCreateResponse createAndSaveOrder(OrderCreationContext context) {
        Order order = Order.create(context.getMember());

        for (int i = 0; i < context.getOrderItems().size(); i++) {
            OrderItemRequest orderItemRequest = context.getOrderItems().get(i);
            GroupBuyOption option = context.getGroupBuyOptions().get(i);
            OrderItem orderItem = OrderItem.create(option, orderItemRequest.quantity());
            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return createOrderResponse(savedOrder);
    }

    /**
     * 회원 조회
     *
     * @param memberId 회원 ID
     * @return 회원 엔티티
     * @throws BusinessException 회원이 존재하지 않을 시
     */
    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 공구 옵션 조회
     *
     * @param optionId 공구 옵션 ID
     * @return 공구 옵션 엔티티 (연관 엔티티 포함)
     * @throws BusinessException 공구 옵션이 존재하지 않을 시
     */
    private GroupBuyOption findGroupBuyOptionById(Long optionId) {
        return groupBuyOptionRepository.findByIdWithDetails(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUPBUY_OPTION_NOT_FOUND, optionId));
    }

    /**
     * 공구 상태 검증
     * 공구가 종료되었는지 확인합니다.
     *
     * @param groupBuy 공구 엔티티
     * @throws BusinessException 공구가 종료된 경우
     */
    private void validateGroupBuyStatus(GroupBuy groupBuy) {
        Instant now = Instant.now();
        Instant endsAt = groupBuy.getEndsAt();

        if (endsAt.isBefore(now)) {
            throw new BusinessException(ErrorCode.GROUPBUY_ENDED);
        }
    }

    /**
     * 재고 검증
     * 전체 재고에서 예약된 수량을 제외한 실제 구매 가능 수량을 확인합니다.
     *
     * @param groupBuyOption 공구 옵션
     * @param requestQuantity 요청 수량
     * @throws BusinessException 재고가 부족한 경우
     */
    private void validateStock(GroupBuyOption groupBuyOption, Integer requestQuantity) {
        Integer totalStock = groupBuyOption.getStock();
        Integer reservedQuantity = stockReservationService.getTotalReservedQuantity(groupBuyOption.getId());
        Integer availableStock = totalStock - reservedQuantity;

        if (availableStock < requestQuantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, requestQuantity, availableStock);
        }
    }

    /**
     * 개인 구매 제한 검증
     * 기존 주문과 새 요청을 합쳐서 개인 제한 초과 여부 확인
     * 현재 예약은 아직 이루어지지 않은 상태이므로 제외
     *
     * @param memberId 회원 ID
     * @param groupBuyOption 공구 옵션
     * @param requestQuantity 요청 수량
     * @throws BusinessException 개인 구매 제한을 초과한 경우
     */
    private void validatePersonalLimit(Long memberId, GroupBuyOption groupBuyOption, Integer requestQuantity) {
        Integer limitQuantityPerMember = groupBuyOption.getGroupBuy().getLimitQuantityPerMember();
        if (limitQuantityPerMember == null || limitQuantityPerMember <= 0) {
            return;
        }

        Integer orderedQuantity = orderItemRepository
                .getTotalOrderedQuantityByMemberAndOption(memberId, groupBuyOption.getId());

        // 현재 예약은 검증 후에 이루어지므로 제외
        int totalQuantity = orderedQuantity + requestQuantity;

        if (totalQuantity > limitQuantityPerMember) {
            throw new BusinessException(ErrorCode.PERSONAL_LIMIT_EXCEEDED, limitQuantityPerMember);
        }
    }

    /**
     * 주문 응답 DTO 생성
     *
     * @param order 주문 엔티티
     * @return 클라이언트용 주문 응답 DTO
     */
    private OrderCreateResponse createOrderResponse(Order order) {
        List<OrderItemResponse> orderItems = order.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        Integer totalAmount = calculateTotalAmount(order.getOrderItems());
        Integer availablePoints = order.getMember().getPoint();
        Integer shippingFee = calculateShippingFee(totalAmount);

        return new OrderCreateResponse(
                order.getId(),
                orderItems,
                totalAmount,
                availablePoints,
                shippingFee
        );
    }

    /**
     * OrderItem을 OrderItemResponse로 변환
     *
     * @param orderItem 주문 아이템 엔티티
     * @return 주문 아이템 응답 DTO
     */
    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        GroupBuyOption option = orderItem.getGroupBuyOption();

        return new OrderItemResponse(
                option.getId(),
                orderItem.getQuantity(),
                option.getGroupBuy().getProduct().getName(),
                option.getProductOption().getName(),
                option.getSalePrice()
        );
    }

    /**
     * 주문 총 금액 계산
     *
     * @param orderItems 주문 아이템 목록
     * @return 총 금액
     */
    private Integer calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToInt(item -> item.getGroupBuyOption().getSalePrice() * item.getQuantity())
                .sum();
    }

    /**
     * 배송비 계산
     * 현재는 일괄 3000원 고정 적용
     *
     * @param totalAmount 총 주문 금액 (현재 미사용)
     * @return 배송비 (3000원 고정)
     */
    private Integer calculateShippingFee(Integer totalAmount) {
        return 3000;
    }

    /**
     * 주문 생성 컨텍스트 클래스
     */
    private static class OrderCreationContext {
        private final Member member;
        private final GroupBuy groupBuy;
        private final List<GroupBuyOption> groupBuyOptions;
        private final List<OrderItemRequest> orderItems;

        public OrderCreationContext(Member member, GroupBuy groupBuy,
                                    List<GroupBuyOption> groupBuyOptions, List<OrderItemRequest> orderItems) {
            this.member = member;
            this.groupBuy = groupBuy;
            this.groupBuyOptions = groupBuyOptions;
            this.orderItems = orderItems;
        }

        public Member getMember() { return member; }
        public GroupBuy getGroupBuy() { return groupBuy; }
        public List<GroupBuyOption> getGroupBuyOptions() { return groupBuyOptions; }
        public List<OrderItemRequest> getOrderItems() { return orderItems; }
    }
}