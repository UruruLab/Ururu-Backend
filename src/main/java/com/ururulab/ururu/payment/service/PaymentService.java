    package com.ururulab.ururu.payment.service;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.ururulab.ururu.global.exception.BusinessException;
    import com.ururulab.ururu.global.exception.error.ErrorCode;
    import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
    import com.ururulab.ururu.groupBuy.dto.common.StockCheckDto;
    import com.ururulab.ururu.groupBuy.event.StockDepletedEvent;
    import com.ururulab.ururu.member.domain.entity.Member;
    import com.ururulab.ururu.member.domain.repository.MemberRepository;
    import com.ururulab.ururu.order.domain.entity.Cart;
    import com.ururulab.ururu.order.domain.entity.Order;
    import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
    import com.ururulab.ururu.order.domain.repository.CartRepository;
    import com.ururulab.ururu.order.domain.repository.OrderRepository;
    import com.ururulab.ururu.order.service.StockReservationService;
    import com.ururulab.ururu.payment.dto.request.PaymentConfirmRequestDto;
    import com.ururulab.ururu.payment.dto.request.PaymentRequestDto;
    import com.ururulab.ururu.payment.dto.request.TossWebhookDto;
    import com.ururulab.ururu.payment.dto.response.PaymentConfirmResponseDto;
    import com.ururulab.ururu.payment.dto.response.PaymentFailResponseDto;
    import com.ururulab.ururu.payment.dto.response.PaymentResponseDto;
    import com.ururulab.ururu.payment.dto.response.TossPaymentResponseDto;
    import com.ururulab.ururu.payment.domain.entity.Payment;
    import com.ururulab.ururu.payment.domain.entity.PointTransaction;
    import com.ururulab.ururu.payment.domain.entity.enumerated.PayMethod;
    import com.ururulab.ururu.payment.domain.entity.enumerated.PaymentStatus;
    import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
    import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
    import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
    import jakarta.servlet.http.HttpServletRequest;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import com.ururulab.ururu.payment.infrastructure.TossPaymentClient;
    import org.springframework.context.ApplicationEventPublisher;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import java.nio.charset.StandardCharsets;
    import java.time.Instant;
    import java.time.ZonedDateTime;
    import java.util.*;
    import java.util.stream.Collectors;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public class PaymentService {

        private static final Integer SHIPPING_FEE = 0; // 배송비 고정
        private static final String TOSS_PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED"; // 토스 웹훅 이벤트 타입
        private static final String DONE = "DONE"; // Toss 결제 상태가 완료인 경우

        private final PaymentRepository paymentRepository;
        private final PointTransactionRepository pointTransactionRepository;
        private final OrderRepository orderRepository;
        private final MemberRepository memberRepository;
        private final StockReservationService stockReservationService;
        private final GroupBuyOptionRepository groupBuyOptionRepository;
        private final CartRepository cartRepository;
        private final TossPaymentClient tossPaymentClient;
        private final ObjectMapper objectMapper;
        private final ApplicationEventPublisher eventPublisher;

        /**
         * 결제 요청 생성
         *
         * @param memberId 회원 ID
         * @param request 결제 요청 정보
         * @return 토스 SDK 실행용 결제 정보
         */
        @Transactional
        public PaymentResponseDto createPaymentRequest(Long memberId, PaymentRequestDto request) {
            Order order = orderRepository.findByIdForUpdate(request.orderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
            validateDuplicatePayment(request.orderId());
            validateOrderForPayment(order, memberId);

            Member member = findMemberById(memberId);
            validatePointBalance(member, request.usePoints());

            order.completePaymentInfo(request.phone(), request.zonecode(), request.address1(), request.address2());

            Integer totalAmount = calculateTotalAmount(order);
            Integer paymentAmount = totalAmount - request.usePoints();

            Payment payment = Payment.create(member, order, totalAmount, paymentAmount, request.usePoints());
            Payment savedPayment = paymentRepository.save(payment);

            String orderName = generateOrderName(order);

            return new PaymentResponseDto(
                    savedPayment.getId(),
                    order.getId(),
                    paymentAmount,
                    orderName,
                    member.getNickname()
            );
        }

        /**
         * 토스 결제 성공 리다이렉트 처리
         *
         * @param paymentKey 토스 결제 키
         * @param orderId 주문 ID
         * @param amount 결제 금액
         */
        public void handlePaymentSuccess(final String paymentKey, final String orderId, @SuppressWarnings("unused") final Integer amount) {
            // 단순히 성공 안내만 함, 실제 처리는 결제 승인에서
            log.debug("결제 성공 리다이렉트 처리 - orderId: {}", orderId);
        }

        /**
         * 토스 결제 실패 리다이렉트 처리
         *
         * @param orderId 주문 ID
         * @param code 실패 코드
         * @param message 실패 메시지
         * @return 실패 정보
         */
        @Transactional
        public PaymentFailResponseDto handlePaymentFail(Long memberId, String orderId, String code, String message) {
            Order order = findOrderById(orderId);
            validateOrderOwnership(order, memberId);
            Payment payment = findPaymentByOrderId(orderId);

            order.changeStatus(OrderStatus.CANCELLED, "결제 실패로 인한 주문 취소");
            payment.markAsFailed();

            releaseReservationsAfterCommit(payment);

            return new PaymentFailResponseDto(code, message, orderId);
        }

        /**
         * 토스 결제 승인
         * 1. 포인트 차감
         * 2. 재고 확정
         * 3. 예약 해제
         *
         * @param paymentId 결제 ID
         * @param request 결제 승인 요청
         * @return 승인 결과
         */
        @Transactional
        public PaymentConfirmResponseDto confirmPayment(Long memberId, Long paymentId, PaymentConfirmRequestDto request) {
            Payment payment = findPaymentById(paymentId);

            validateOrderOwnership(payment.getOrder(), memberId);

            if (!payment.isPending()) {
                throw new BusinessException(ErrorCode.PAYMENT_NOT_PENDING);
            }

            if (!payment.getAmount().equals(request.amount())) {
                throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
            }

            TossPaymentResponseDto tossResponse = tossPaymentClient.confirm(
                    request.paymentKey(),
                    payment.getOrder().getId(),
                    payment.getAmount(),
                    "payment-confirm-" + paymentId
            );

            validateTossPayment(tossResponse, request.paymentKey(), payment);

            PayMethod payMethod = PayMethod.from(tossResponse.method(), tossResponse.easyPayProvider());
            Instant approvedAt = ZonedDateTime.parse(tossResponse.approvedAt()).toInstant();

            payment.updatePaymentInfo(request.paymentKey(), payMethod, payment.getAmount());
            payment.markAsPaid(approvedAt);
            payment.getOrder().changeStatus(OrderStatus.ORDERED, "결제 승인 완료");

            completePaymentProcessing(payment);

            return new PaymentConfirmResponseDto(paymentId, PaymentStatus.PAID, approvedAt);
        }

        /**
         * 토스 웹훅 검증 및 처리 (컨트롤러에서 호출)
         *
         * @param request HTTP 요청 (Raw body 읽기용)
         */
        @Transactional
        public void handleTossWebhookWithValidation(HttpServletRequest request) {
            try {
                String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                if (rawBody == null || rawBody.trim().isEmpty()) {
                    throw new BusinessException(ErrorCode.INVALID_JSON, "웹훅 본문이 비어있습니다");
                }

                TossWebhookDto webhook = objectMapper.readValue(rawBody, TossWebhookDto.class);

                handleTossWebhook(webhook);

            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.WEBHOOK_PROCESSING_FAILED);
            }
        }

        /**
         * 토스 웹훅 처리
         *
         * @param webhook 웹훅 데이터
         */
        @Transactional
        public void handleTossWebhook(TossWebhookDto webhook) {
            if (!TOSS_PAYMENT_STATUS_CHANGED.equals(webhook.eventType())) {
                return;
            }

            if (webhook.data() == null || webhook.data().paymentKey() == null) {
                throw new BusinessException(ErrorCode.INVALID_JSON);
            }

            TossPaymentResponseDto tossPayment = tossPaymentClient.getPayment(webhook.data().paymentKey());
            if (tossPayment == null || tossPayment.orderId() == null) {
                throw new BusinessException(ErrorCode.WEBHOOK_PROCESSING_FAILED);
            }

            Payment payment = paymentRepository.findByOrderId(tossPayment.orderId()).orElse(null);

            if (payment == null || payment.isPaid()) {
                return;
            }

            if (DONE.equals(tossPayment.status())) {
                validateTossPayment(tossPayment, webhook.data().paymentKey(), payment);
                PayMethod payMethod = PayMethod.from(tossPayment.method(), tossPayment.easyPayProvider());
                Instant approvedAt = ZonedDateTime.parse(tossPayment.approvedAt()).toInstant();
                payment.updatePaymentInfo(tossPayment.paymentKey(), payMethod, payment.getAmount());
                payment.markAsPaid(approvedAt);
                payment.getOrder().changeStatus(OrderStatus.ORDERED, "웹훅을 통한 결제 상태 동기화");

                completePaymentProcessing(payment);
            }
        }

        /**
         * paymentKey로 결제 상태 조회 (프론트 폴링용)
         *
         * @param paymentKey 토스페이먼츠 결제 키
         * @param memberId 회원 ID (권한 검증용)
         * @return 결제 상태 정보
         */
        @Transactional(readOnly = true)
        public PaymentConfirmResponseDto getPaymentStatusByKey(String paymentKey, Long memberId) {
            Payment payment = paymentRepository.findByPaymentKeyWithDetails(paymentKey)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

            if (!payment.getMember().getId().equals(memberId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }

            log.debug("결제 상태 조회 - status: {}", payment.getStatus());

            return new PaymentConfirmResponseDto(
                    payment.getId(),
                    payment.getStatus(),
                    payment.getPaidAt() != null ? payment.getPaidAt() : null
            );
        }

        /**
         * 결제 완료 후 처리 (포인트 차감 + 재고 확정 + 예약 해제)
         * confirmPayment와 웹훅에서 공통 사용
         *
         * @param payment 결제 정보
         */
        private void completePaymentProcessing(Payment payment) {
            // 재고 차감 + 예약 해제
            payment.getOrder().getOrderItems().forEach(item -> {
                Long optionId = item.getGroupBuyOption().getId();
                Integer quantity = item.getQuantity();

                // 실재고 차감
                int updatedRows = groupBuyOptionRepository.decreaseStock(optionId, quantity);
                if (updatedRows == 0) {
                    throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
                }

            });

            // 포인트 차감
            processPointUsage(payment.getMember(), payment.getPoint());

            removeOrderedItemsFromCart(payment);

            // 재고 소진 체크 및 이벤트 발행
            handleStockDepletionCheck(payment);

            releaseReservationsAfterCommit(payment);

            log.debug("결제 완료 처리 완료 - paymentId: {}, 포인트: {}, 주문아이템: {}개",
                    payment.getId(), payment.getPoint(), payment.getOrder().getOrderItems().size());
        }

        /**
         * 재고 소진 체크 및 이벤트 발행 처리
         *
         * @param payment 결제 정보
         */
        private void handleStockDepletionCheck(Payment payment) {
            // 옵션 ID 수집
            List<Long> optionIds = payment.getOrder().getOrderItems().stream()
                    .map(item -> item.getGroupBuyOption().getId())
                    .collect(Collectors.toList());

            // 재고 상태와 groupBuyId를 한 번에 조회
            List<StockCheckDto> stockResults =
                    groupBuyOptionRepository.getStockAndGroupBuyIdsByOptionIds(optionIds);

            // 재고가 0이 된 공동구매 ID 수집
            Set<Long> groupBuyIdsToCheck = stockResults.stream()
                    .filter(dto -> dto.stock() != null && dto.stock() == 0)
                    .map(StockCheckDto::groupBuyId)
                    .collect(Collectors.toSet());

            // 재고가 0이 된 공동구매가 있을 때만 이벤트 발행
            if (!groupBuyIdsToCheck.isEmpty()) {
                StockDepletedEvent event = new StockDepletedEvent(groupBuyIdsToCheck);
                eventPublisher.publishEvent(event);
                log.info("재고 소진 이벤트 발행 - paymentId: {}, 대상 공동구매: {}",
                        payment.getId(), groupBuyIdsToCheck);
            }
        }


        private void validateDuplicatePayment(String orderId) {
            if (paymentRepository.findByOrderId(orderId).isPresent()) {
                throw new BusinessException(ErrorCode.PAYMENT_ALREADY_EXISTS);
            }
        }

        private void validateTossPayment(TossPaymentResponseDto response, String paymentKey, Payment payment) {
            if (response == null
                    || !paymentKey.equals(response.paymentKey())
                    || !payment.getOrder().getId().equals(response.orderId())
                    || !payment.getAmount().equals(response.totalAmount())
                    || !DONE.equals(response.status())
                    || response.approvedAt() == null) {
                throw new BusinessException(ErrorCode.PAYMENT_APPROVAL_FAILED);
            }
        }

        private void releaseReservationsAfterCommit(Payment payment) {
            Runnable release = () -> payment.getOrder().getOrderItems().forEach(item ->
                    stockReservationService.releaseReservation(
                            item.getGroupBuyOption().getId(), payment.getMember().getId()));

            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                release.run();
                            }
                        });
            } else {
                release.run();
            }
        }

        private void validateOrderOwnership(Order order, Long memberId) {
            if (!order.getMember().getId().equals(memberId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
        }

        private Order findOrderById(String orderId) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        }

        private void validateOrderForPayment(Order order, Long memberId) {
            if (!order.getMember().getId().equals(memberId)) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new BusinessException(ErrorCode.ORDER_NOT_PENDING);
            }
        }

        private Member findMemberById(Long memberId) {
            return memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        }

        private void validatePointBalance(Member member, Integer usePoints) {
            if (member.getPoint() < usePoints) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
            }
        }

        private Payment findPaymentById(Long paymentId) {
            return paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        }

        private Payment findPaymentByOrderId(String orderId) {
            return paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        }

        private Integer calculateTotalAmount(Order order) {
            return order.getOrderItems().stream()
                    .mapToInt(item -> item.getGroupBuyOption().getSalePrice() * item.getQuantity())
                    .sum() + SHIPPING_FEE; // 배송비 고정
        }

        private void processPointUsage(Member member, Integer usePoints) {
            if (usePoints > 0) {
                int updatedRows = memberRepository.decreasePoints(member.getId(), usePoints);
                if (updatedRows == 0) {
                    throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
                }

                PointTransaction pointTransaction = PointTransaction.createUsed(
                        member, PointSource.GROUPBUY, usePoints, "공동구매 결제"
                );
                pointTransactionRepository.save(pointTransaction);
            }
        }

        private String generateOrderName(Order order) {
            String firstProductName = order.getOrderItems().get(0)
                    .getGroupBuyOption().getGroupBuy().getProduct().getName();
            int totalItems = order.getOrderItems().size();

            if (totalItems == 1) {
                return firstProductName;
            }
            return firstProductName + " 외 " + (totalItems - 1) + "건";
        }
        /**
         * 결제 완료된 옵션들을 장바구니에서 제거
         */
        private void removeOrderedItemsFromCart(Payment payment) {
            Long memberId = payment.getMember().getId();

            Optional<Cart> cartOpt = cartRepository.findByMemberIdWithCartItems(memberId);

            if (cartOpt.isEmpty()) {
                log.debug("장바구니가 없음 - 회원ID: {}", memberId);
                return;
            }

            Cart cart = cartOpt.get();

            payment.getOrder().getOrderItems().forEach(orderItem -> {
                Long optionId = orderItem.getGroupBuyOption().getId();

                cart.getCartItems().stream()
                        .filter(cartItem -> cartItem.getGroupBuyOption().getId().equals(optionId))
                        .findFirst()
                        .ifPresent(cartItem -> {
                            Long cartItemId = cartItem.getId();
                            cart.removeItem(cartItemId);
                            log.debug("장바구니에서 제거 - 회원ID: {}, 옵션ID: {}, 아이템ID: {}",
                                    memberId, optionId, cartItemId);
                        });
            });
        }

    }
