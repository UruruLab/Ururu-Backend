package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.order.service.StockReservationService;
import com.ururulab.ururu.payment.domain.dto.request.PaymentConfirmRequestDto;
import com.ururulab.ururu.payment.domain.dto.request.PaymentRequestDto;
import com.ururulab.ururu.payment.domain.dto.request.TossWebhookDto;
import com.ururulab.ururu.payment.domain.dto.response.PaymentConfirmResponseDto;
import com.ururulab.ururu.payment.domain.dto.response.PaymentFailResponseDto;
import com.ururulab.ururu.payment.domain.dto.response.PaymentResponseDto;
import com.ururulab.ururu.payment.domain.dto.response.TossPaymentResponseDto;
import com.ururulab.ururu.payment.domain.entity.Payment;
import com.ururulab.ururu.payment.domain.entity.PointTransaction;
import com.ururulab.ururu.payment.domain.entity.enumerated.PayMethod;
import com.ururulab.ururu.payment.domain.entity.enumerated.PaymentStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final StockReservationService stockReservationService;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final RestClient restClient;

    @Value("${toss.payments.secret-key}")
    private String tossSecretKey;

    @Value("${toss.payments.base-url}")
    private String tossBaseUrl;

    /**
     * 결제 요청 생성
     *
     * @param memberId 회원 ID
     * @param request 결제 요청 정보
     * @return 토스 SDK 실행용 결제 정보
     */
    @Transactional
    public PaymentResponseDto createPaymentRequest(Long memberId, PaymentRequestDto request) {
        validateDuplicatePayment(request.orderId());

        Order order = findOrderById(request.orderId());
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
    public void handlePaymentSuccess(final String paymentKey, final String orderId, final Integer amount) {
        // 단순히 성공 안내만 함, 실제 처리는 결제 승인에서
        log.debug("결제 성공 리다이렉트 처리 - paymentKey: {}, orderId: {}", paymentKey, orderId);
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
    public PaymentFailResponseDto handlePaymentFail(String orderId, String code, String message) {
        Order order = findOrderById(orderId);
        Payment payment = findPaymentByOrderId(orderId);

        order.changeStatus(OrderStatus.CANCELLED, "결제 실패로 인한 주문 취소");
        payment.markAsFailed();

        order.getOrderItems().forEach(item -> {
            Long optionId = item.getGroupBuyOption().getId();
            stockReservationService.releaseReservation(optionId, order.getMember().getId());
        });

        return new PaymentFailResponseDto(code, message, orderId);
    }

    /**
     * 토스 결제 승인
     *
     * 1. 포인트 차감
     * 2. 재고 확정
     * 3. 예약 해제
     *
     * @param paymentId 결제 ID
     * @param request 결제 승인 요청
     * @return 승인 결과
     */
    @Transactional
    public PaymentConfirmResponseDto confirmPayment(Long paymentId, PaymentConfirmRequestDto request) {
        Payment payment = findPaymentById(paymentId);

        TossPaymentResponseDto tossResponse = callTossPaymentConfirmApi(
                request.paymentKey(),
                payment.getOrder().getId(),
                request.amount()
        );

        PayMethod payMethod = PayMethod.from(tossResponse.method(), tossResponse.easyPayProvider());
        ZonedDateTime approvedAt = ZonedDateTime.parse(tossResponse.approvedAt());

        payment.updatePaymentInfo(request.paymentKey(), payMethod, request.amount());
        payment.markAsPaid(approvedAt);

        payment.getOrder().changeStatus(OrderStatus.ORDERED, "결제 승인 완료");
        processPointUsage(payment.getMember(), payment.getPoint());

        payment.getOrder().getOrderItems().forEach(item -> {
            Long optionId = item.getGroupBuyOption().getId();
            Integer quantity = item.getQuantity();

            int updatedRows = groupBuyOptionRepository.decreaseStock(optionId, quantity);
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }

            stockReservationService.releaseReservation(optionId, payment.getMember().getId());
        });

        return new PaymentConfirmResponseDto(paymentId, PaymentStatus.PAID, approvedAt.toInstant());
    }

    /**
     * 토스 웹훅 처리
     *
     * @param webhook 웹훅 데이터
     */
    @Transactional
    public void handleTossWebhook(TossWebhookDto webhook) {
        if (!"PAYMENT_STATUS_CHANGED".equals(webhook.eventType())) {
            return;
        }

        Payment payment = paymentRepository.findByPaymentKeyWithDetails(webhook.data().paymentKey())
                .orElse(null);

        if (payment == null || payment.isPaid()) {
            return;
        }

        if ("DONE".equals(webhook.data().status())) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            payment.markAsPaid(now);
            payment.getOrder().changeStatus(OrderStatus.ORDERED, "웹훅을 통한 결제 상태 동기화");
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

        log.debug("결제 상태 조회 - paymentKey: {}, status: {}", paymentKey, payment.getStatus());

        return new PaymentConfirmResponseDto(
                payment.getId(),
                payment.getStatus(),
                payment.getPaidAt() != null ? payment.getPaidAt().toInstant() : null
        );
    }

    private void validateDuplicatePayment(String orderId) {
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
    }

    private TossPaymentResponseDto callTossPaymentConfirmApi(String paymentKey, String orderId, Integer amount) {
        try {
            String auth = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes());

            Map<String, Object> requestBody = Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount
            );

            return restClient.post()
                    .uri(tossBaseUrl + "/v1/payments/confirm")
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(TossPaymentResponseDto.class);

        } catch (Exception e) {
            log.error("토스 API 호출 실패 - paymentKey: {}, orderId: {}", paymentKey, orderId, e);
            throw new BusinessException(ErrorCode.TOSS_API_CALL_FAILED);
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
                .sum() + 3000; // 배송비 고정
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

    private void restorePointsIfUsed(Payment payment) {
        if (payment.getPoint() > 0) {
            memberRepository.increasePoints(payment.getMember().getId(), payment.getPoint());

            PointTransaction pointTransaction = PointTransaction.createEarned(
                    payment.getMember(), PointSource.REFUND, payment.getPoint(), "결제 실패로 인한 포인트 복구"
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
}