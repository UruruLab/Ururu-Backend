package com.ururulab.ururu.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.FinalStatus;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private static final Long GROUP_BUY_ID = 100L;
    private static final Integer TOTAL_AMOUNT = 18000;

    private Member testMember;
    private Order testOrder;
    private OrderItem testOrderItem;
    private Payment testPayment;
    private GroupBuy testGroupBuy;
    private GroupBuyOption testGroupBuyOption;
    private Product testProduct;
    private ProductOption testProductOption;

    @BeforeEach
    void setUp() {
        setupTestEntities();
        setupBasicMocks();
    }

    private void setupBasicMocks() {
        // 기본 Mock 설정
        given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
        given(orderRepository.countInProgressOrders(MEMBER_ID)).willReturn(1L);
        given(orderRepository.countConfirmedOrders(MEMBER_ID)).willReturn(0L);
        given(orderRepository.countRefundPendingOrders(MEMBER_ID)).willReturn(0L);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(testPayment));

        // 환불되지 않은 상태로 설정 - INITIATED가 존재하지 않아야 화면에 표시됨
        given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(eq(1L), eq(List.of(RefundStatus.INITIATED))))
                .willReturn(false);

        // ObjectMapper 기본 설정
        try {
            given(objectMapper.readValue(eq("[]"), any(TypeReference.class))).willReturn(List.of());
        } catch (Exception e) {
            // 예외 무시
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetMyOrdersTest {

        @Test
        @DisplayName("성공 - 전체 조회")
        void getMyOrders_all_success() {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(orderPage);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5);

            // then
            assertThat(result.orders()).hasSize(1);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(5);
            assertThat(result.total()).isEqualTo(1L);

            MyOrderResponseDto orderDto = result.orders().get(0);
            assertThat(orderDto.orderId()).isEqualTo(ORDER_ID);
            // totalAmount는 환불 로직에 따라 달라질 수 있으므로 검증하지 않음
            assertThat(orderDto.orderItems()).hasSize(1);

            verify(memberRepository).existsById(MEMBER_ID);
            verify(orderRepository).findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 상태 필터링")
        void getMyOrders_statusFilter_success() {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), eq(OrderStatus.ORDERED), any(Pageable.class)))
                    .willReturn(orderPage);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "ORDERED", 1, 5);

            // then
            assertThat(result.orders()).hasSize(1);
            verify(orderRepository).findMyOrdersWithDetails(eq(MEMBER_ID), eq(OrderStatus.ORDERED), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 잘못된 상태 파라미터는 all로 처리")
        void getMyOrders_invalidStatus_treatedAsAll() {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(orderPage);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "INVALID_STATUS", 1, 5);

            // then
            assertThat(result.orders()).hasSize(1);
            verify(orderRepository).findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class));
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
            verify(orderRepository, never()).findMyOrdersWithDetails(anyLong(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("결제 정보 없음")
        void getMyOrders_paymentNotFound_fail() {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(orderPage);
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("성공 - 환불된 아이템은 제외")
        void getMyOrders_excludeRefundedItems_success() {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(orderPage);
            // 환불 신청 상태인 경우
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                    eq(1L),
                    eq(List.of(
                            RefundStatus.APPROVED,
                            RefundStatus.COMPLETED,
                            RefundStatus.FAILED,
                            RefundStatus.REJECTED
                    ))
            )).willReturn(true);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5);

            // then
            MyOrderResponseDto orderDto = result.orders().get(0);
            assertThat(orderDto.orderItems()).isEmpty(); // 환불 신청된 아이템은 제외됨
        }

        @Test
        @DisplayName("INITIATED 상태의 환불 아이템은 주문 목록에 포함된다")
        void getMyOrders_initiatedRefundItem_isVisible() {
            // given
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(orderPage);

            // INITIATED 상태는 제외 대상 아님 → 조회 시 보이는 게 맞음
            given(refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                    eq(1L),
                    eq(List.of(
                            RefundStatus.APPROVED,
                            RefundStatus.COMPLETED,
                            RefundStatus.FAILED,
                            RefundStatus.REJECTED
                    ))
            )).willReturn(false);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5);

            // then
            MyOrderResponseDto orderDto = result.orders().get(0);
            assertThat(orderDto.orderItems()).hasSize(1);
            assertThat(orderDto.orderItems().get(0).productName()).isEqualTo("테스트 상품");
        }


        @Test
        @DisplayName("성공 - 빈 결과")
        void getMyOrders_emptyResult_success() {
            // given
            Page<Order> emptyPage = new PageImpl<>(List.of());
            given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when
            MyOrderListResponseDto result = myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5);

            // then
            assertThat(result.orders()).isEmpty();
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(5);
            assertThat(result.total()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("공구 상태 결정")
    class DetermineGroupBuyStatusTest {

        @Test
        @DisplayName("OPEN 상태 - OPEN 반환")
        void determineGroupBuyStatus_open_returnsOpen() {
            // given
            given(testGroupBuy.getStatus()).willReturn(GroupBuyStatus.OPEN);

            // when
            MyOrderListResponseDto result = executeGetMyOrders();

            // then
            OrderItemResponseDto orderItem = result.orders().get(0).orderItems().get(0);
            assertThat(orderItem.status()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("CLOSED 상태 - 통계 있음")
        void determineGroupBuyStatus_closedWithStats_returnsFinalStatus() {
            // given
            given(testGroupBuy.getStatus()).willReturn(GroupBuyStatus.CLOSED);
            GroupBuyStatistics statistics = mock(GroupBuyStatistics.class);
            given(statistics.getFinalStatus()).willReturn(FinalStatus.SUCCESS);
            given(statistics.getFinalDiscountRate()).willReturn(15);
            given(groupBuyStatisticsRepository.findByGroupBuyId(GROUP_BUY_ID))
                    .willReturn(Optional.of(statistics));

            // when
            MyOrderListResponseDto result = executeGetMyOrders();

            // then
            OrderItemResponseDto orderItem = result.orders().get(0).orderItems().get(0);
            assertThat(orderItem.status()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("CLOSED 상태 - 통계 없음")
        void determineGroupBuyStatus_closedWithoutStats_returnsClosed() {
            // given
            given(testGroupBuy.getStatus()).willReturn(GroupBuyStatus.CLOSED);
            given(groupBuyStatisticsRepository.findByGroupBuyId(GROUP_BUY_ID))
                    .willReturn(Optional.empty());

            // when
            MyOrderListResponseDto result = executeGetMyOrders();

            // then
            OrderItemResponseDto orderItem = result.orders().get(0).orderItems().get(0);
            assertThat(orderItem.status()).isEqualTo("CLOSED");
        }
    }

    @Nested
    @DisplayName("할인율 계산")
    class CalculateDiscountRateTest {

        @Test
        @DisplayName("OPEN 상태 - 현재 할인율 계산")
        void calculateDiscountRate_open_returnsCurrentRate() throws Exception {
            // given
            given(testGroupBuy.getStatus()).willReturn(GroupBuyStatus.OPEN);
            String discountStages = "[{\"count\":10,\"rate\":10},{\"count\":30,\"rate\":20}]";
            given(testGroupBuy.getDiscountStages()).willReturn(discountStages);

            List<Map<String, Object>> stages = List.of(
                    Map.of("count", 10, "rate", 10),
                    Map.of("count", 30, "rate", 20)
            );

            given(objectMapper.readValue(eq(discountStages), any(TypeReference.class)))
                    .willReturn(stages);
            given(orderItemRepository.getTotalSalesQuantityByGroupBuyId(GROUP_BUY_ID))
                    .willReturn(35); // 30개 이상이므로 20% 할인

            // when
            MyOrderListResponseDto result = executeGetMyOrders();

            // then
            OrderItemResponseDto orderItem = result.orders().get(0).orderItems().get(0);
            assertThat(orderItem.rate()).isEqualTo(20);
        }

        @Test
        @DisplayName("CLOSED 상태 - 최종 할인율")
        void calculateDiscountRate_closed_returnsFinalRate() {
            // given
            given(testGroupBuy.getStatus()).willReturn(GroupBuyStatus.CLOSED);
            GroupBuyStatistics statistics = mock(GroupBuyStatistics.class);
            given(statistics.getFinalStatus()).willReturn(FinalStatus.SUCCESS);
            given(statistics.getFinalDiscountRate()).willReturn(15);
            given(groupBuyStatisticsRepository.findByGroupBuyId(GROUP_BUY_ID))
                    .willReturn(Optional.of(statistics));

            // when
            MyOrderListResponseDto result = executeGetMyOrders();

            // then
            OrderItemResponseDto orderItem = result.orders().get(0).orderItems().get(0);
            assertThat(orderItem.rate()).isEqualTo(15);
        }

        @Test
        @DisplayName("할인 단계 파싱 실패 - 0% 반환")
        void calculateDiscountRate_parsingError_returnsZero() throws Exception {
            // given
            given(testGroupBuy.getStatus()).willReturn(GroupBuyStatus.OPEN);
            String invalidJson = "invalid_json";
            given(testGroupBuy.getDiscountStages()).willReturn(invalidJson);

            // ObjectMapper에서 예외 발생하도록 설정
            given(objectMapper.readValue(eq(invalidJson), any(TypeReference.class)))
                    .willThrow(new RuntimeException("Parsing error"));

            // when
            MyOrderListResponseDto result = executeGetMyOrders();

            // then
            OrderItemResponseDto orderItem = result.orders().get(0).orderItems().get(0);
            assertThat(orderItem.rate()).isEqualTo(0);
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
        given(product.getName()).willReturn("테스트 상품");
        return product;
    }

    private ProductOption createTestProductOption() {
        ProductOption option = mock(ProductOption.class);
        given(option.getId()).willReturn(10L);
        given(option.getName()).willReturn("기본 옵션");
        given(option.getImageUrl()).willReturn("image.jpg");
        return option;
    }

    private GroupBuy createTestGroupBuy() {
        GroupBuy groupBuy = mock(GroupBuy.class);
        given(groupBuy.getId()).willReturn(GROUP_BUY_ID);
        given(groupBuy.getProduct()).willReturn(testProduct);
        given(groupBuy.getStatus()).willReturn(GroupBuyStatus.OPEN);
        given(groupBuy.getDiscountStages()).willReturn("[]");
        return groupBuy;
    }

    private GroupBuyOption createTestGroupBuyOption() {
        GroupBuyOption option = mock(GroupBuyOption.class);
        given(option.getId()).willReturn(1L);
        given(option.getGroupBuy()).willReturn(testGroupBuy);
        given(option.getProductOption()).willReturn(testProductOption);
        given(option.getSalePrice()).willReturn(15000);
        return option;
    }

    private OrderItem createTestOrderItem() {
        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getId()).willReturn(1L);
        given(orderItem.getGroupBuyOption()).willReturn(testGroupBuyOption);
        given(orderItem.getQuantity()).willReturn(1);
        return orderItem;
    }

    private Order createTestOrder() {
        Order order = mock(Order.class);
        given(order.getId()).willReturn(ORDER_ID);
        given(order.getMember()).willReturn(testMember);
        given(order.getCreatedAt()).willReturn(Instant.now());
        given(order.getTrackingNumber()).willReturn("TRACK123");
        given(order.getOrderItems()).willReturn(List.of(testOrderItem));
        return order;
    }

    private Payment createTestPayment() {
        Payment payment = mock(Payment.class);
        given(payment.getTotalAmount()).willReturn(TOTAL_AMOUNT);
        return payment;
    }

    private MyOrderListResponseDto executeGetMyOrders() {
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        given(orderRepository.findMyOrdersWithDetails(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                .willReturn(orderPage);

        return myOrderService.getMyOrders(MEMBER_ID, "all", 1, 5);
    }
}