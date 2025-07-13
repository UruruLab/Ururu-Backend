package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
import com.ururulab.ururu.payment.domain.repository.RefundRepository;
import com.ururulab.ururu.payment.dto.request.RefundProcessRequestDto;
import com.ururulab.ururu.payment.dto.request.RefundRequestDto;
import com.ururulab.ururu.payment.dto.response.RefundCreateResponseDto;
import com.ururulab.ururu.payment.dto.response.RefundProcessResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService 테스트")
class RefundServiceTest {

    @InjectMocks
    private RefundService refundService;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private GroupBuyOptionRepository groupBuyOptionRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private RefundTestFixture.RefundTestScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = RefundTestFixture.createCompleteScenario();
    }

    @Nested
    @DisplayName("환불 요청 생성")
    class CreateRefundRequestTest {

        @Test
        @DisplayName("성공 - 운송장 등록 전 자동 승인")
        void createRefundRequest_beforeTrackingRegistered_autoApprove() {
            // given
            RefundRequestDto request = RefundTestFixture.createChangeOfMindRequest();

            setupBasicMocks();
            given(refundRepository.findActiveManualRefundByOrderAndMember(scenario.order.getId(), scenario.member.getId()))
                    .willReturn(Optional.empty());
            given(orderItemRepository.findRefundableItemsByOrderId(scenario.order.getId()))
                    .willReturn(List.of(scenario.orderItem));
            given(refundRepository.save(any(Refund.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // 자동 승인 시 필요한 Mock들 추가
            given(memberRepository.increasePoints(eq(scenario.member.getId()), anyInt())).willReturn(1);
            given(groupBuyOptionRepository.increaseStock(eq(scenario.groupBuyOption.getId()), anyInt())).willReturn(1);

            // 디버깅용 로그
            System.out.println("Test Order ID: " + scenario.order.getId());
            System.out.println("Test Payment ID: " + scenario.payment.getId());

            // when
            RefundCreateResponseDto result = refundService.createRefundRequest(
                    scenario.member.getId(), scenario.order.getId(), request);

            // then
            assertThat(result.status()).isEqualTo(RefundStatus.APPROVED); // 자동 승인
            assertThat(result.type()).isEqualTo(RefundType.CHANGE_OF_MIND);
            assertThat(result.amount()).isPositive(); // 일단 양수인지만 확인

            verify(memberRepository).increasePoints(eq(scenario.member.getId()), anyInt());
            verify(pointTransactionRepository).save(any());
            verify(groupBuyOptionRepository).increaseStock(eq(scenario.groupBuyOption.getId()), anyInt());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void createRefundRequest_orderNotFound_fail() {
            // given
            RefundRequestDto request = RefundTestFixture.createChangeOfMindRequest();

            given(orderRepository.findById("non-existent-order")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> refundService.createRefundRequest(
                    scenario.member.getId(), "non-existent-order", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 환불 가능한 아이템 없음")
        void createRefundRequest_noRefundableItems_fail() {
            // given
            RefundRequestDto request = RefundTestFixture.createChangeOfMindRequest();

            // 이 테스트에 필요한 최소 Mock만 설정
            given(orderRepository.findById(eq("test-order-id")))
                    .willReturn(Optional.of(scenario.order));
            given(refundRepository.findActiveManualRefundByOrderAndMember(scenario.order.getId(), scenario.member.getId()))
                    .willReturn(Optional.empty());
            given(orderItemRepository.findRefundableItemsByOrderId(scenario.order.getId()))
                    .willReturn(List.of()); // 환불 가능한 아이템 없음

            // when & then
            assertThatThrownBy(() -> refundService.createRefundRequest(
                    scenario.member.getId(), scenario.order.getId(), request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CART_ITEMS_EMPTY);
        }

        @Test
        @DisplayName("실패 - 이미 진행 중인 환불 요청")
        void createRefundRequest_duplicateRequest_fail() {
            // given
            RefundRequestDto request = RefundTestFixture.createChangeOfMindRequest();
            Refund existingRefund = RefundTestFixture.createRefund("existing-refund",
                    scenario.payment, RefundType.CHANGE_OF_MIND, "기존 요청",
                    10000, 500, RefundStatus.INITIATED);

            // 이 테스트에 필요한 최소 Mock만 설정
            given(orderRepository.findById(eq("test-order-id")))
                    .willReturn(Optional.of(scenario.order));
            given(refundRepository.findActiveManualRefundByOrderAndMember(
                    scenario.order.getId(), scenario.member.getId()))
                    .willReturn(Optional.of(existingRefund));

            // when & then
            assertThatThrownBy(() -> refundService.createRefundRequest(
                    scenario.member.getId(), scenario.order.getId(), request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_REFUND_REQUEST);
        }
    }

    @Nested
    @DisplayName("환불 요청 처리")
    class ProcessRefundRequestTest {

        @Test
        @DisplayName("성공 - 판매자 승인")
        void processRefundRequest_approve_success() {
            // given
            String refundId = "refund-123";
            Long sellerId = scenario.seller.getId();
            RefundProcessRequestDto request = RefundTestFixture.createApproveRequest();

            // RefundItem을 제대로 설정한 Refund 생성
            Refund refund = RefundTestFixture.createRefundWithItems(refundId, scenario.payment,
                    RefundType.CHANGE_OF_MIND, "단순 변심", 15000, 1000, RefundStatus.INITIATED, scenario.orderItem);

            given(refundRepository.findByIdWithDetails(refundId)).willReturn(Optional.of(refund));
            given(memberRepository.increasePoints(eq(scenario.member.getId()), eq(1000))).willReturn(1);
            given(groupBuyOptionRepository.increaseStock(eq(scenario.groupBuyOption.getId()), eq(2))).willReturn(1);

            // when
            RefundProcessResponseDto result = refundService.processRefundRequest(sellerId, refundId, request);

            // then
            assertThat(result.refundId()).isEqualTo(refundId);
            assertThat(result.status()).isEqualTo(RefundStatus.APPROVED);
            assertThat(result.rejectReason()).isNull();

            verify(memberRepository).increasePoints(eq(scenario.member.getId()), eq(1000));
            verify(pointTransactionRepository).save(any());
            verify(groupBuyOptionRepository).increaseStock(eq(scenario.groupBuyOption.getId()), eq(2));
        }

        @Test
        @DisplayName("성공 - 판매자 거절")
        void processRefundRequest_reject_success() {
            // given
            String refundId = "refund-123";
            Long sellerId = scenario.seller.getId();
            String rejectReason = "재고 부족으로 환불 불가";
            RefundProcessRequestDto request = RefundTestFixture.createRejectRequest(rejectReason);

            Refund refund = RefundTestFixture.createRefundWithItems(refundId, scenario.payment,
                    RefundType.CHANGE_OF_MIND, "단순 변심", 15000, 1000, RefundStatus.INITIATED, scenario.orderItem);

            given(refundRepository.findByIdWithDetails(refundId)).willReturn(Optional.of(refund));

            // when
            RefundProcessResponseDto result = refundService.processRefundRequest(sellerId, refundId, request);

            // then
            assertThat(result.refundId()).isEqualTo(refundId);
            assertThat(result.status()).isEqualTo(RefundStatus.REJECTED);
            assertThat(result.rejectReason()).isEqualTo(rejectReason);

            // 환불 처리가 실행되지 않았는지 확인
            verify(memberRepository, never()).increasePoints(anyLong(), anyInt());
            verify(pointTransactionRepository, never()).save(any());
            verify(groupBuyOptionRepository, never()).increaseStock(anyLong(), anyInt());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 환불")
        void processRefundRequest_refundNotFound_fail() {
            // given
            String refundId = "non-existent-refund";
            Long sellerId = scenario.seller.getId();
            RefundProcessRequestDto request = RefundTestFixture.createApproveRequest();

            given(refundRepository.findByIdWithDetails(refundId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> refundService.processRefundRequest(sellerId, refundId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.REFUND_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 권한 없는 판매자")
        void processRefundRequest_accessDenied_fail() {
            // given
            String refundId = "refund-123";
            Long otherSellerId = 999L; // 다른 판매자
            RefundProcessRequestDto request = RefundTestFixture.createApproveRequest();

            Refund refund = RefundTestFixture.createRefundWithItems(refundId, scenario.payment,
                    RefundType.CHANGE_OF_MIND, "단순 변심", 15000, 1000, RefundStatus.INITIATED, scenario.orderItem);

            given(refundRepository.findByIdWithDetails(refundId)).willReturn(Optional.of(refund));

            // when & then
            assertThatThrownBy(() -> refundService.processRefundRequest(otherSellerId, refundId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("실패 - 이미 처리된 환불")
        void processRefundRequest_alreadyProcessed_fail() {
            // given
            String refundId = "refund-123";
            Long sellerId = scenario.seller.getId();
            RefundProcessRequestDto request = RefundTestFixture.createApproveRequest();

            Refund refund = RefundTestFixture.createRefundWithItems(refundId, scenario.payment,
                    RefundType.CHANGE_OF_MIND, "단순 변심", 15000, 1000, RefundStatus.APPROVED, scenario.orderItem); // 이미 승인됨

            given(refundRepository.findByIdWithDetails(refundId)).willReturn(Optional.of(refund));

            // when & then
            assertThatThrownBy(() -> refundService.processRefundRequest(sellerId, refundId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.REFUND_ALREADY_PROCESSED);
        }
    }

    private void setupBasicMocks() {
        given(orderRepository.findById(eq("test-order-id")))
                .willReturn(Optional.of(scenario.order));

        // 더 명시적인 Mock 설정
        given(paymentRepository.findByOrderId(eq("test-order-id")))
                .willReturn(Optional.of(scenario.payment));

        // 디버깅용 로그
        System.out.println("Mocking Order ID: " + scenario.order.getId());
        System.out.println("Mocking Payment: " + scenario.payment);
    }
}