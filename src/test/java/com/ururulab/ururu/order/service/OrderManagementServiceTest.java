package com.ururulab.ururu.order.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.order.dto.request.ShippingInfoUpdateRequest;
import com.ururulab.ururu.order.dto.response.ShippingInfoUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderManagementService 테스트")
class OrderManagementServiceTest {

    @InjectMocks
    private OrderManagementService orderManagementService;

    @Mock
    private OrderRepository orderRepository;

    private OrderManagementTestFixture.TestScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = OrderManagementTestFixture.createTestScenario();
    }

    @Nested
    @DisplayName("배송 정보 등록")
    class UpdateShippingInfoTest {

        @Test
        @DisplayName("성공 - 정상적인 운송장 등록")
        void updateShippingInfo_success() {
            // given
            String orderId = scenario.order.getId();
            Long sellerId = scenario.seller.getId();
            ShippingInfoUpdateRequest request = OrderManagementTestFixture.createValidShippingRequest();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(scenario.order));

            // when
            ShippingInfoUpdateResponse result = orderManagementService.updateShippingInfo(sellerId, orderId, request);

            // then
            assertThat(result.orderId()).isEqualTo(orderId);
            assertThat(result.trackingNumber()).isEqualTo(request.trackingNumber());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void updateShippingInfo_orderNotFound_fail() {
            // given
            String nonExistentOrderId = "non-existent-order";
            Long sellerId = scenario.seller.getId();
            ShippingInfoUpdateRequest request = OrderManagementTestFixture.createValidShippingRequest();

            given(orderRepository.findById(nonExistentOrderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderManagementService.updateShippingInfo(sellerId, nonExistentOrderId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 권한 없는 판매자")
        void updateShippingInfo_accessDenied_fail() {
            // given
            String orderId = scenario.order.getId();
            Long otherSellerId = 999L; // 다른 판매자
            ShippingInfoUpdateRequest request = OrderManagementTestFixture.createValidShippingRequest();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(scenario.order));

            // when & then
            assertThatThrownBy(() -> orderManagementService.updateShippingInfo(otherSellerId, orderId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("실패 - ORDERED가 아닌 주문 상태")
        void updateShippingInfo_orderNotShippable_fail() {
            // given
            String orderId = scenario.order.getId();
            Long sellerId = scenario.seller.getId();
            ShippingInfoUpdateRequest request = OrderManagementTestFixture.createValidShippingRequest();

            // Order 상태를 CANCELLED로 변경
            OrderManagementTestFixture.setFieldValue(scenario.order, "status", OrderStatus.CANCELLED);

            given(orderRepository.findById(orderId)).willReturn(Optional.of(scenario.order));

            // when & then
            assertThatThrownBy(() -> orderManagementService.updateShippingInfo(sellerId, orderId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_NOT_SHIPPABLE);
        }

        @Test
        @DisplayName("실패 - 이미 운송장이 등록된 주문")
        void updateShippingInfo_trackingAlreadyRegistered_fail() {
            // given
            String orderId = scenario.order.getId();
            Long sellerId = scenario.seller.getId();
            ShippingInfoUpdateRequest request = OrderManagementTestFixture.createValidShippingRequest();

            // 이미 운송장이 등록된 상태로 설정
            OrderManagementTestFixture.setFieldValue(scenario.order, "trackingNumber", "existing-tracking");

            given(orderRepository.findById(orderId)).willReturn(Optional.of(scenario.order));

            // when & then
            assertThatThrownBy(() -> orderManagementService.updateShippingInfo(sellerId, orderId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TRACKING_ALREADY_REGISTERED);
        }
    }
}