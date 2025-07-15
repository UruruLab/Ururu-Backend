package com.ururulab.ururu.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.CartRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.order.service.StockReservationService;
import com.ururulab.ururu.payment.dto.request.PaymentConfirmRequestDto;
import com.ururulab.ururu.payment.dto.request.PaymentRequestDto;
import com.ururulab.ururu.payment.dto.response.PaymentConfirmResponseDto;
import com.ururulab.ururu.payment.dto.response.PaymentFailResponseDto;
import com.ururulab.ururu.payment.dto.response.PaymentResponseDto;
import com.ururulab.ururu.payment.dto.response.TossPaymentResponseDto;
import com.ururulab.ururu.payment.domain.entity.Payment;
import com.ururulab.ururu.payment.domain.entity.enumerated.PaymentStatus;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
import com.ururulab.ururu.product.domain.entity.Product;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 테스트")
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private GroupBuyOptionRepository groupBuyOptionRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RestClient restClient;

    @Mock
    private ObjectMapper objectMapper;

    private static final Long MEMBER_ID = 1L;
    private static final String ORDER_ID = "ORDER123";
    private static final String PAYMENT_KEY = "PAYMENT_KEY_123";
    private static final Integer TOTAL_AMOUNT = 15000;
    private static final Integer PAYMENT_AMOUNT = 10000;
    private static final Integer USE_POINTS = 5000;
    private static final Integer MEMBER_POINTS = 10000;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "tossSecretKey", "test_secret_key");
        ReflectionTestUtils.setField(paymentService, "tossBaseUrl", "https://api.tosspayments.com");
    }

    @Nested
    @DisplayName("결제 요청 생성")
    class CreatePaymentRequestTest {

        @Test
        @DisplayName("성공")
        void createPaymentRequest_success() {
            // given
            PaymentRequestDto request = createPaymentRequest();
            Member member = createMember();
            Order order = createOrder(member);
            Payment savedPayment = createPayment(member, order);

            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

            // when
            PaymentResponseDto result = paymentService.createPaymentRequest(MEMBER_ID, request);

            // then
            assertThat(result.paymentId()).isEqualTo(1L);
            assertThat(result.orderId()).isEqualTo(ORDER_ID);
            assertThat(result.amount()).isEqualTo(PAYMENT_AMOUNT);
            assertThat(result.customerName()).isEqualTo("테스트유저");

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment capturedPayment = paymentCaptor.getValue();
            assertThat(capturedPayment.getTotalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(capturedPayment.getAmount()).isEqualTo(PAYMENT_AMOUNT);
            assertThat(capturedPayment.getPoint()).isEqualTo(USE_POINTS);
            assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("중복 결제 실패")
        void createPaymentRequest_duplicatePayment_fail() {
            // given
            PaymentRequestDto request = createPaymentRequest();
            Payment existingPayment = createPayment(createMember(), createOrder(createMember()));

            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(existingPayment));

            // when & then
            assertThatThrownBy(() -> paymentService.createPaymentRequest(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_ALREADY_EXISTS);

            verify(orderRepository, never()).findById(anyString());
            verify(memberRepository, never()).findById(anyLong());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("포인트 부족 실패")
        void createPaymentRequest_insufficientPoints_fail() {
            // given
            PaymentRequestDto request = new PaymentRequestDto(
                    ORDER_ID, 15000, "010-1234-5678", "12345", "서울시", "상세주소"
            );
            Member member = createMember();
            Order order = createOrder(member);

            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> paymentService.createPaymentRequest(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INSUFFICIENT_POINTS);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 주문")
        void createPaymentRequest_orderNotFound_fail() {
            // given
            PaymentRequestDto request = createPaymentRequest();

            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.createPaymentRequest(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

            verify(memberRepository, never()).findById(anyLong());
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("결제 승인")
    class ConfirmPaymentTest {

        @Test
        @DisplayName("성공")
        void confirmPayment_success() {
            // given
            Long paymentId = 1L;
            PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(PAYMENT_KEY, PAYMENT_AMOUNT);

            Member member = createMember();
            Order order = createOrder(member);
            addOrderItemToOrder(order);
            Payment payment = createPayment(member, order);

            TossPaymentResponseDto tossResponse = new TossPaymentResponseDto(
                    PAYMENT_KEY, ORDER_ID, "카드", null, "DONE", "2024-01-01T10:00:00+09:00", PAYMENT_AMOUNT
            );

            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
            mockRestClientChain(tossResponse);
            given(memberRepository.decreasePoints(MEMBER_ID, USE_POINTS)).willReturn(1);
            given(groupBuyOptionRepository.decreaseStock(anyLong(), anyInt())).willReturn(1);
            given(cartRepository.findByMemberIdWithCartItems(MEMBER_ID)).willReturn(Optional.empty());

            // when
            PaymentConfirmResponseDto result = paymentService.confirmPayment(paymentId, request);

            // then
            assertThat(result.paymentId()).isEqualTo(paymentId);
            assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
            assertThat(result.paidAt()).isNotNull();

            verify(memberRepository).decreasePoints(MEMBER_ID, USE_POINTS);
            verify(pointTransactionRepository).save(argThat(transaction ->
                    transaction.getAmount().equals(USE_POINTS) &&
                            transaction.getMember().getId().equals(MEMBER_ID) &&
                            transaction.isUsed()
            ));
            verify(stockReservationService).releaseReservation(1L, MEMBER_ID);
            verify(groupBuyOptionRepository).decreaseStock(1L, 1);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getPaymentKey()).isEqualTo(PAYMENT_KEY);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        }

        @Test
        @DisplayName("존재하지 않는 결제")
        void confirmPayment_paymentNotFound_fail() {
            // given
            Long paymentId = 999L;
            PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(PAYMENT_KEY, PAYMENT_AMOUNT);

            given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);

            verify(restClient, never()).post();
            verify(memberRepository, never()).decreasePoints(anyLong(), anyInt());
        }

        @Test
        @DisplayName("이미 결제된 상태에서 승인 시도")
        void confirmPayment_alreadyPaid_fail() {
            // given
            Long paymentId = 1L;
            PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(PAYMENT_KEY, PAYMENT_AMOUNT);

            Member member = createMember();
            Order order = createOrder(member);
            Payment payment = createPayment(member, order);
            payment.markAsPaid(Instant.now());

            // Payment 엔티티의 상태를 확인하여 이미 결제된 상태로 설정하는 것이 맞는지 확인
            // 실제 서비스 로직에서 어떤 검증을 하는지에 따라 결정
            given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

            // when & then
            // 실제로는 토스 API 호출이 발생할 수 있으므로 해당 예외를 검증
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_NOT_PENDING);
        }
    }

    @Nested
    @DisplayName("결제 실패 처리")
    class HandlePaymentFailTest {

        @Test
        @DisplayName("성공")
        void handlePaymentFail_success() {
            // given
            String code = "FAIL_CODE";
            String message = "결제 실패";

            Member member = createMember();
            Order order = createOrder(member);
            Payment payment = createPayment(member, order);

            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));

            // when
            PaymentFailResponseDto result = paymentService.handlePaymentFail(ORDER_ID, code, message);

            // then
            assertThat(result.errorCode()).isEqualTo(code);
            assertThat(result.errorMessage()).isEqualTo(message);
            assertThat(result.orderId()).isEqualTo(ORDER_ID);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

            verify(stockReservationService).releaseReservation(1L, MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("토스 웹훅 검증")
    class HandleTossWebhookTest {

        @Test
        @DisplayName("서명 누락 실패")
        void handleTossWebhookWithValidation_missingSignature_fail() throws Exception {
            // given
            String rawBody = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\"}";
            HttpServletRequest request = mock(HttpServletRequest.class);
            given(request.getInputStream()).willReturn(new MockServletInputStream(rawBody));

            // when & then
            assertThatThrownBy(() -> paymentService.handleTossWebhookWithValidation(request, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_SIGNATURE);

            verify(objectMapper, never()).readValue(anyString(), eq(Object.class));
        }

        @Test
        @DisplayName("빈 본문 실패")
        void handleTossWebhookWithValidation_emptyBody_fail() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            given(request.getInputStream()).willReturn(new MockServletInputStream(""));

            // when & then
            assertThatThrownBy(() -> paymentService.handleTossWebhookWithValidation(request, "signature"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_JSON);

            verify(objectMapper, never()).readValue(anyString(), eq(Object.class));
        }
    }

    private PaymentRequestDto createPaymentRequest() {
        return new PaymentRequestDto(
                ORDER_ID, USE_POINTS, "010-1234-5678", "12345", "서울시", "상세주소"
        );
    }

    private Member createMember() {
        Member member = Member.of(
                "테스트유저", "test@example.com", SocialProvider.KAKAO,
                "social123", Gender.NONE, null, null, null, Role.NORMAL
        );
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        ReflectionTestUtils.setField(member, "point", MEMBER_POINTS);
        return member;
    }

    private Order createOrder(Member member) {
        Order order = Order.create(member);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);

        // OrderItem 추가하여 총액 계산이 가능하도록 함
        OrderItem orderItem = mock(OrderItem.class);
        GroupBuyOption groupBuyOption = mock(GroupBuyOption.class);
        GroupBuy groupBuy = mock(GroupBuy.class);
        Product product = mock(Product.class);

        lenient().when(orderItem.getGroupBuyOption()).thenReturn(groupBuyOption);
        lenient().when(groupBuyOption.getSalePrice()).thenReturn(15000);
        lenient().when(orderItem.getQuantity()).thenReturn(1);
        lenient().when(groupBuyOption.getId()).thenReturn(1L);
        lenient().when(groupBuyOption.getGroupBuy()).thenReturn(groupBuy);
        lenient().when(groupBuy.getProduct()).thenReturn(product);
        lenient().when(product.getName()).thenReturn("테스트 상품");

        if (order.getOrderItems() == null) {
            ReflectionTestUtils.setField(order, "orderItems", new ArrayList<>());
        }
        order.getOrderItems().add(orderItem);

        return order;
    }

    private void addOrderItemToOrder(Order order) {
        // 이미 createOrder에서 OrderItem을 추가했으므로 중복 추가하지 않음
        if (order.getOrderItems().isEmpty()) {
            OrderItem orderItem = mock(OrderItem.class);
            GroupBuyOption groupBuyOption = mock(GroupBuyOption.class);

            lenient().when(orderItem.getGroupBuyOption()).thenReturn(groupBuyOption);
            lenient().when(groupBuyOption.getId()).thenReturn(1L);
            lenient().when(orderItem.getQuantity()).thenReturn(1);

            order.getOrderItems().add(orderItem);
        }
    }

    private Payment createPayment(Member member, Order order) {
        Payment payment = Payment.create(member, order, TOTAL_AMOUNT, PAYMENT_AMOUNT, USE_POINTS);
        ReflectionTestUtils.setField(payment, "id", 1L);
        return payment;
    }

    private void mockRestClientChain(TossPaymentResponseDto tossResponse) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.post()).willReturn(uriSpec);
        given(uriSpec.uri(anyString())).willReturn(bodySpec);
        given(bodySpec.header(anyString(), anyString())).willReturn(bodySpec);
        given(bodySpec.body(any(Map.class))).willReturn(bodySpec);
        given(bodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(TossPaymentResponseDto.class)).willReturn(tossResponse);
    }

    private static class MockServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public MockServletInputStream(String data) {
            this.inputStream = new ByteArrayInputStream(data.getBytes());
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}