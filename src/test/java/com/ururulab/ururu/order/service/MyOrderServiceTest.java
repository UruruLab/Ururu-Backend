package com.ururulab.ururu.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyStatisticsRepository;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.order.dto.response.MyOrderListResponseDto;
import com.ururulab.ururu.order.dto.response.MyOrderResponseDto;
import com.ururulab.ururu.order.dto.response.OrderItemResponseDto;
import com.ururulab.ururu.payment.domain.entity.Payment;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.RefundItemRepository;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyOrderService 테스트")
class MyOrderServiceTest {

    @InjectMocks
    private MyOrderService myOrderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private GroupBuyStatisticsRepository groupBuyStatisticsRepository;

    @Mock
    private RefundItemRepository refundItemRepository;

    @Mock
    private ObjectMapper objectMapper;

    private static final Long MEMBER_ID = 1L;
    private static final String ORDER_ID = "ORDER123";
    private static final Integer TOTAL_AMOUNT = 18000;

    private Member testMember;
    private Order testOrder;
    private OrderItem testOrderItem;
    private GroupBuyOption testGroupBuyOption;
    private GroupBuy testGroupBuy;
    private Product testProduct;
    private ProductOption testProductOption;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        setupTestEntities();
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetMyOrdersTest {

        @Test
        @DisplayName("성공 - 진행중 주문 조회 (inProgress)")
        void getMyOrders_inProgress_success() throws JsonProcessingException {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(orderRepository.countInProgressOrders(MEMBER_ID)).willReturn(1L);
            given(orderRepository.countConfirmedOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.countRefundPendingOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.findInProgressOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class)))
                    .willReturn(orderPage);
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(anyLong(), any())).willReturn(false);
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));

            // ObjectMapper 모킹
            List<Map<String, Object>> stages = List.of(Map.of("count", 10, "rate", 5));
            lenient().when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(stages);
            lenient().when(orderItemRepository.getTotalQuantityByGroupBuyId(testGroupBuy.getId())).thenReturn(25);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "inProgress", 1, 5);

            // then
            assertThat(result.orders()).hasSize(1);
            verify(orderRepository).findInProgressOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 확정 주문 조회 (confirmed)")
        void getMyOrders_confirmed_success() throws JsonProcessingException {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(orderRepository.countInProgressOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.countConfirmedOrders(MEMBER_ID)).willReturn(1L);
            given(orderRepository.countRefundPendingOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.findConfirmedOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class)))
                    .willReturn(orderPage);
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(anyLong(), any())).willReturn(false);
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));

            // ObjectMapper 모킹
            List<Map<String, Object>> stages = List.of(Map.of("count", 10, "rate", 5));
            lenient().when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(stages);
            lenient().when(orderItemRepository.getTotalQuantityByGroupBuyId(testGroupBuy.getId())).thenReturn(25);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "confirmed", 1, 5);

            // then
            assertThat(result.orders()).hasSize(1);
            verify(orderRepository).findConfirmedOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 환불 대기 주문 조회 (refundPending)")
        void getMyOrders_refundPending_success() throws JsonProcessingException {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(orderRepository.countInProgressOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.countConfirmedOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.countRefundPendingOrders(MEMBER_ID)).willReturn(1L);
            given(orderRepository.findRefundPendingOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class)))
                    .willReturn(orderPage);
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(anyLong(), any())).willReturn(false);
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));

            // ObjectMapper 모킹
            List<Map<String, Object>> stages = List.of(Map.of("count", 10, "rate", 5));
            lenient().when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(stages);
            lenient().when(orderItemRepository.getTotalQuantityByGroupBuyId(testGroupBuy.getId())).thenReturn(25);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "refundPending", 1, 5);

            // then
            assertThat(result.orders()).hasSize(1);
            verify(orderRepository).findRefundPendingOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원")
        void getMyOrders_memberNotFound_fail() {
            // given
            given(memberRepository.existsById(MEMBER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

            verify(memberRepository).existsById(MEMBER_ID);
            verify(orderRepository, never()).findAllOrdersWithDetails(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 잘못된 상태 파라미터는 all로 처리")
        void getMyOrders_invalidStatus_treatedAsAll() throws JsonProcessingException {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(orderRepository.countInProgressOrders(MEMBER_ID)).willReturn(1L);
            given(orderRepository.countConfirmedOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.countRefundPendingOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.findAllOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class)))
                    .willReturn(orderPage);
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(anyLong(), any())).willReturn(false);
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));

            // ObjectMapper 모킹
            List<Map<String, Object>> stages = List.of(Map.of("count", 10, "rate", 5));
            lenient().when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(stages);
            lenient().when(orderItemRepository.getTotalQuantityByGroupBuyId(testGroupBuy.getId())).thenReturn(25);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "INVALID_STATUS", 1, 5);

            // then
            assertThat(result.orders()).hasSize(1);
            verify(orderRepository).findAllOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        void getMyOrders_emptyResult_success() {
            // given
            Page<Order> emptyPage = new PageImpl<>(List.of());

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(orderRepository.countInProgressOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.countConfirmedOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.countRefundPendingOrders(MEMBER_ID)).willReturn(0L);
            given(orderRepository.findAllOrdersWithDetails(eq(MEMBER_ID), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5);

            // then
            assertThat(result.orders()).isEmpty();
            assertThat(result.inProgress()).isEqualTo(0);
            assertThat(result.confirmed()).isEqualTo(0);
            assertThat(result.refundPending()).isEqualTo(0);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(5);
            assertThat(result.total()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("할인율 계산 테스트")
    class DiscountRateCalculationTest {

        @Test
        @DisplayName("진행 중인 공구의 현재 할인율 계산")
        void calculateCurrentDiscountRate_openGroupBuy_success() throws Exception {
            // given
            List<Map<String, Object>> stages = List.of(
                    Map.of("count", 10, "rate", 5),
                    Map.of("count", 20, "rate", 10),
                    Map.of("count", 30, "rate", 15)
            );

            lenient().when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(stages);
            lenient().when(orderItemRepository.getTotalQuantityByGroupBuyId(testGroupBuy.getId())).thenReturn(25);

            // when
            Method method = MyOrderService.class.getDeclaredMethod("calculateCurrentDiscountRate", GroupBuy.class);
            method.setAccessible(true);
            Integer result = (Integer) method.invoke(myOrderService, testGroupBuy);

            // then
            assertThat(result).isEqualTo(10);
        }

        @Test
        @DisplayName("종료된 공구의 최종 할인율 조회")
        void calculateDiscountRate_closedGroupBuy_success() {
            // given
            lenient().when(testGroupBuy.getStatus()).thenReturn(GroupBuyStatus.CLOSED);
            GroupBuyStatistics statistics = mock(GroupBuyStatistics.class);
            lenient().when(statistics.getFinalDiscountRate()).thenReturn(15);
            lenient().when(groupBuyStatisticsRepository.findByGroupBuyId(testGroupBuy.getId()))
                    .thenReturn(Optional.of(statistics));

            // when
            try {
                Method method = MyOrderService.class.getDeclaredMethod("calculateDiscountRate", GroupBuy.class);
                method.setAccessible(true);
                Integer result = (Integer) method.invoke(myOrderService, testGroupBuy);

                // then
                assertThat(result).isEqualTo(15);
            } catch (Exception e) {
                fail("메서드 호출 실패", e);
            }
        }

        @Test
        @DisplayName("JSON 파싱 실패 시 0 반환")
        void calculateCurrentDiscountRate_jsonParseError_returns0() throws Exception {
            // given
            lenient().when(testGroupBuy.getDiscountStages()).thenReturn("invalid json");
            // ObjectMapper에서 예외 발생 시 catch 블록에서 0 반환하도록 하는 테스트
            lenient().when(objectMapper.readValue(eq("invalid json"), any(TypeReference.class)))
                    .thenThrow(new RuntimeException("JSON parse error"));

            // when
            Method method = MyOrderService.class.getDeclaredMethod("calculateCurrentDiscountRate", GroupBuy.class);
            method.setAccessible(true);
            Integer result = (Integer) method.invoke(myOrderService, testGroupBuy);

            // then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("환불 상태 확인")
    class RefundStatusTest {

        @Test
        @DisplayName("환불되지 않은 아이템 확인")
        void isNotRefunded_success() {
            // given
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                    eq(testOrderItem.getId()),
                    eq(List.of(RefundStatus.APPROVED, RefundStatus.COMPLETED, RefundStatus.FAILED, RefundStatus.REJECTED))
            )).willReturn(false);

            // when
            try {
                Method method = MyOrderService.class.getDeclaredMethod("isNotRefunded", OrderItem.class);
                method.setAccessible(true);
                boolean result = (boolean) method.invoke(myOrderService, testOrderItem);

                // then
                assertThat(result).isTrue();
            } catch (Exception e) {
                fail("메서드 호출 실패", e);
            }
        }

        @Test
        @DisplayName("환불된 아이템 확인")
        void isNotRefunded_refunded_returnsFalse() {
            // given
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                    eq(testOrderItem.getId()),
                    eq(List.of(RefundStatus.APPROVED, RefundStatus.COMPLETED, RefundStatus.FAILED, RefundStatus.REJECTED))
            )).willReturn(true);

            // when
            try {
                Method method = MyOrderService.class.getDeclaredMethod("isNotRefunded", OrderItem.class);
                method.setAccessible(true);
                boolean result = (boolean) method.invoke(myOrderService, testOrderItem);

                // then
                assertThat(result).isFalse();
            } catch (Exception e) {
                fail("메서드 호출 실패", e);
            }
        }
    }

    @Nested
    @DisplayName("현재 금액 계산")
    class CurrentAmountCalculationTest {

        @Test
        @DisplayName("환불 없는 경우 전체 금액")
        void calculateCurrentAmount_noRefund_success() {
            // given
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));
            // isRefunded = false 이므로 환불되지 않음
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                    eq(testOrderItem.getId()),
                    eq(List.of(RefundStatus.INITIATED))
            )).willReturn(true); // INITIATED가 있으면 isRefunded = false

            // when
            try {
                Method method = MyOrderService.class.getDeclaredMethod("calculateCurrentAmount", Order.class);
                method.setAccessible(true);
                Integer result = (Integer) method.invoke(myOrderService, testOrder);

                // then
                assertThat(result).isEqualTo(TOTAL_AMOUNT); // 환불 없으므로 전체 금액
            } catch (Exception e) {
                fail("메서드 호출 실패", e);
            }
        }

        @Test
        @DisplayName("환불 있는 경우 차감된 금액")
        void calculateCurrentAmount_withRefund_success() {
            // given
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));
            // isRefunded = true 이므로 환불됨
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                    eq(testOrderItem.getId()),
                    eq(List.of(RefundStatus.INITIATED))
            )).willReturn(false); // INITIATED가 없으면 isRefunded = true

            // when
            try {
                Method method = MyOrderService.class.getDeclaredMethod("calculateCurrentAmount", Order.class);
                method.setAccessible(true);
                Integer result = (Integer) method.invoke(myOrderService, testOrder);

                // then
                assertThat(result).isEqualTo(TOTAL_AMOUNT - 15000); // 환불된 금액 제외
            } catch (Exception e) {
                fail("메서드 호출 실패", e);
            }
        }

        @Test
        @DisplayName("결제 정보 없는 경우 예외")
        void calculateCurrentAmount_paymentNotFound_throwsException() {
            // given
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            try {
                Method method = MyOrderService.class.getDeclaredMethod("calculateCurrentAmount", Order.class);
                method.setAccessible(true);

                assertThatThrownBy(() -> method.invoke(myOrderService, testOrder))
                        .getCause()
                        .isInstanceOf(BusinessException.class)
                        .extracting(ex -> ((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
            } catch (Exception e) {
                fail("메서드 호출 실패", e);
            }
        }
    }

    private void setupTestEntities() {
        testMember = createTestMember();
        testProduct = createTestProduct();
        testProductOption = createTestProductOption();
        testGroupBuy = createTestGroupBuy();
        testGroupBuyOption = createTestGroupBuyOption();
        testOrderItem = createTestOrderItem();
        testOrder = createTestOrder();
        testPayment = createTestPayment();
    }

    private Member createTestMember() {
        Member member = Member.of(
                "테스트유저", "test@example.com", SocialProvider.KAKAO,
                "social123", Gender.NONE, null, null, null, Role.NORMAL
        );
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private Product createTestProduct() {
        Product product = mock(Product.class);
        lenient().when(product.getName()).thenReturn("테스트 상품");
        return product;
    }

    private ProductOption createTestProductOption() {
        ProductOption option = mock(ProductOption.class);
        lenient().when(option.getId()).thenReturn(10L);
        lenient().when(option.getName()).thenReturn("기본 옵션");
        lenient().when(option.getImageUrl()).thenReturn("image.jpg");
        return option;
    }

    private GroupBuy createTestGroupBuy() {
        GroupBuy groupBuy = mock(GroupBuy.class);
        lenient().when(groupBuy.getId()).thenReturn(1L);
        lenient().when(groupBuy.getProduct()).thenReturn(testProduct);
        lenient().when(groupBuy.getStatus()).thenReturn(GroupBuyStatus.OPEN);
        lenient().when(groupBuy.getDiscountStages()).thenReturn(
                "[{\"count\":10,\"rate\":5},{\"count\":20,\"rate\":10},{\"count\":30,\"rate\":15}]"
        );
        return groupBuy;
    }

    private GroupBuyOption createTestGroupBuyOption() {
        GroupBuyOption option = mock(GroupBuyOption.class);
        lenient().when(option.getId()).thenReturn(1L);
        lenient().when(option.getGroupBuy()).thenReturn(testGroupBuy);
        lenient().when(option.getProductOption()).thenReturn(testProductOption);
        lenient().when(option.getSalePrice()).thenReturn(15000);
        return option;
    }

    private OrderItem createTestOrderItem() {
        OrderItem orderItem = mock(OrderItem.class);
        lenient().when(orderItem.getId()).thenReturn(1L);
        lenient().when(orderItem.getGroupBuyOption()).thenReturn(testGroupBuyOption);
        lenient().when(orderItem.getQuantity()).thenReturn(1);
        return orderItem;
    }

    private Order createTestOrder() {
        Order order = mock(Order.class);
        lenient().when(order.getId()).thenReturn(ORDER_ID);
        lenient().when(order.getMember()).thenReturn(testMember);
        lenient().when(order.getStatus()).thenReturn(OrderStatus.ORDERED);
        lenient().when(order.getCreatedAt()).thenReturn(Instant.now());
        lenient().when(order.getOrderItems()).thenReturn(List.of(testOrderItem));
        lenient().when(order.getTrackingNumber()).thenReturn(null);
        return order;
    }

    private Payment createTestPayment() {
        Payment payment = mock(Payment.class);
        lenient().when(payment.getTotalAmount()).thenReturn(TOTAL_AMOUNT);
        return payment;
    }
}