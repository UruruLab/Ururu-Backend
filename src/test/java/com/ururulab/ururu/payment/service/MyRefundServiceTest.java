package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.RefundItem;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;
import com.ururulab.ururu.payment.domain.repository.RefundRepository;
import com.ururulab.ururu.payment.dto.response.MyRefundListResponseDto;
import com.ururulab.ururu.payment.dto.response.MyRefundResponseDto;
import com.ururulab.ururu.payment.dto.response.RefundItemResponseDto;
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

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyRefundService 테스트")
class MyRefundServiceTest {

    @InjectMocks
    private MyRefundService myRefundService;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private MemberRepository memberRepository;

    private static final Long MEMBER_ID = 1L;
    private static final String REFUND_ID = "REFUND123";
    private static final Integer REFUND_AMOUNT = 15000;

    private Member testMember;
    private Refund testRefund;
    private RefundItem testRefundItem;
    private OrderItem testOrderItem;
    private GroupBuyOption testGroupBuyOption;
    private GroupBuy testGroupBuy;
    private Product testProduct;
    private ProductOption testProductOption;

    @BeforeEach
    void setUp() {
        setupTestEntities();
    }

    @Nested
    @DisplayName("환불 내역 조회")
    class GetMyRefundsTest {

        @Test
        @DisplayName("성공 - 전체 조회")
        void getMyRefunds_all_success() {
            // given
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            assertThat(result.refunds()).hasSize(1);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(5);
            assertThat(result.total()).isEqualTo(1L);

            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.refundId()).isEqualTo(REFUND_ID);
            assertThat(refundDto.type()).isEqualTo(RefundType.CHANGE_OF_MIND);
            assertThat(refundDto.reason()).isEqualTo("단순 변심");
            assertThat(refundDto.status()).isEqualTo(RefundStatus.APPROVED);
            assertThat(refundDto.totalAmount()).isEqualTo(REFUND_AMOUNT);
            assertThat(refundDto.refundItems()).hasSize(1);

            RefundItemResponseDto itemDto = refundDto.refundItems().get(0);
            assertThat(itemDto.productName()).isEqualTo("테스트 상품");
            assertThat(itemDto.optionName()).isEqualTo("기본 옵션");
            assertThat(itemDto.quantity()).isEqualTo(1);
            assertThat(itemDto.price()).isEqualTo(15000);

            verify(memberRepository).existsById(MEMBER_ID);
            verify(refundRepository).findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 상태 필터링")
        void getMyRefunds_statusFilter_success() {
            // given
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(
                    eq(MEMBER_ID), eq(RefundStatus.APPROVED), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "APPROVED", 1, 5);

            // then
            assertThat(result.refunds()).hasSize(1);
            verify(refundRepository).findProcessedRefundsByMemberId(
                    eq(MEMBER_ID), eq(RefundStatus.APPROVED), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 잘못된 상태 파라미터는 all로 처리")
        void getMyRefunds_invalidStatus_treatedAsAll() {
            // given
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "INVALID_STATUS", 1, 5);

            // then
            assertThat(result.refunds()).hasSize(1);
            verify(refundRepository).findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원")
        void getMyRefunds_memberNotFound_fail() {
            // given
            given(memberRepository.existsById(MEMBER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

            verify(memberRepository).existsById(MEMBER_ID);
            verify(refundRepository, never()).findProcessedRefundsByMemberId(anyLong(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        void getMyRefunds_emptyResult_success() {
            // given
            Page<Refund> emptyPage = new PageImpl<>(List.of());

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            assertThat(result.refunds()).isEmpty();
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(5);
            assertThat(result.total()).isEqualTo(0L);
        }

        @Test
        @DisplayName("성공 - 여러 환불 아이템")
        void getMyRefunds_multipleRefundItems_success() {
            // given
            RefundItem secondRefundItem = createSecondRefundItem();
            given(testRefund.getRefundItems()).willReturn(List.of(testRefundItem, secondRefundItem));
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.refundItems()).hasSize(2);

            RefundItemResponseDto firstItem = refundDto.refundItems().get(0);
            assertThat(firstItem.productName()).isEqualTo("테스트 상품");
            assertThat(firstItem.optionName()).isEqualTo("기본 옵션");

            RefundItemResponseDto secondItem = refundDto.refundItems().get(1);
            assertThat(secondItem.productName()).isEqualTo("테스트 상품2");
            assertThat(secondItem.optionName()).isEqualTo("추가 옵션");
        }
    }

        @Nested
    @DisplayName("환불 상태별 테스트")
    class RefundStatusTest {

        @Test
        @DisplayName("APPROVED 상태 환불")
        void getMyRefunds_approvedStatus_success() {
            // given
            ReflectionTestUtils.setField(testRefund, "status", RefundStatus.APPROVED);
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.status()).isEqualTo(RefundStatus.APPROVED);
            assertThat(refundDto.rejectionReason()).isNull();
            assertThat(refundDto.refundAt()).isNull();
        }

        @Test
        @DisplayName("REJECTED 상태 환불")
        void getMyRefunds_rejectedStatus_success() {
            // given
            ReflectionTestUtils.setField(testRefund, "status", RefundStatus.REJECTED);
            ReflectionTestUtils.setField(testRefund, "rejectReason", "재고 부족");
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.status()).isEqualTo(RefundStatus.REJECTED);
            assertThat(refundDto.rejectionReason()).isEqualTo("재고 부족");
        }

        @Test
        @DisplayName("COMPLETED 상태 환불")
        void getMyRefunds_completedStatus_success() {
            // given
            Instant refundAt = Instant.now();
            ReflectionTestUtils.setField(testRefund, "status", RefundStatus.COMPLETED);
            ReflectionTestUtils.setField(testRefund, "refundedAt", refundAt);
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.status()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(refundDto.refundAt()).isEqualTo(refundAt);
        }

        @Test
        @DisplayName("FAILED 상태 환불")
        void getMyRefunds_failedStatus_success() {
            // given
            ReflectionTestUtils.setField(testRefund, "status", RefundStatus.FAILED);
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.status()).isEqualTo(RefundStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("환불 타입별 테스트")
    class RefundTypeTest {

        @Test
        @DisplayName("공구 실패 환불")
        void getMyRefunds_groupBuyFailedType_success() {
            // given
            given(testRefund.getType()).willReturn(RefundType.GROUPBUY_FAILED);
            given(testRefund.getReason()).willReturn("공구 최소 수량 미달");
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.type()).isEqualTo(RefundType.GROUPBUY_FAILED);
            assertThat(refundDto.reason()).isEqualTo("공구 최소 수량 미달");
        }

        @Test
        @DisplayName("불량품 환불")
        void getMyRefunds_defectiveProductType_success() {
            // given
            ReflectionTestUtils.setField(testRefund, "type", RefundType.DEFECTIVE_PRODUCT);
            ReflectionTestUtils.setField(testRefund, "reason", "제품 불량으로 인한 환불");
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.type()).isEqualTo(RefundType.DEFECTIVE_PRODUCT);
            assertThat(refundDto.reason()).isEqualTo("제품 불량으로 인한 환불");
        }

        @Test
        @DisplayName("배송 문제 환불")
        void getMyRefunds_deliveryIssueType_success() {
            // given
            ReflectionTestUtils.setField(testRefund, "type", RefundType.DELIVERY_ISSUE);
            ReflectionTestUtils.setField(testRefund, "reason", "배송 지연으로 인한 환불");
            Page<Refund> refundPage = new PageImpl<>(List.of(testRefund));

            given(memberRepository.existsById(MEMBER_ID)).willReturn(true);
            given(refundRepository.findProcessedRefundsByMemberId(eq(MEMBER_ID), isNull(), any(Pageable.class)))
                    .willReturn(refundPage);

            // when
            MyRefundListResponseDto result = myRefundService.getMyRefunds(MEMBER_ID, "all", 1, 5);

            // then
            MyRefundResponseDto refundDto = result.refunds().get(0);
            assertThat(refundDto.type()).isEqualTo(RefundType.DELIVERY_ISSUE);
            assertThat(refundDto.reason()).isEqualTo("배송 지연으로 인한 환불");
        }
    }

    private void setupTestEntities() {
        testMember = createTestMember();
        testProduct = createTestProduct();
        testProductOption = createTestProductOption();
        testGroupBuy = createTestGroupBuy();
        testGroupBuyOption = createTestGroupBuyOption();
        testOrderItem = createTestOrderItem();
        testRefundItem = createTestRefundItem();
        testRefund = createTestRefund();
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
        lenient().when(groupBuy.getProduct()).thenReturn(testProduct);
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

    private RefundItem createTestRefundItem() {
        RefundItem refundItem = mock(RefundItem.class);
        lenient().when(refundItem.getOrderItem()).thenReturn(testOrderItem);
        return refundItem;
    }

    private RefundItem createSecondRefundItem() {
        // 두 번째 상품 정보 생성
        Product secondProduct = mock(Product.class);
        lenient().when(secondProduct.getName()).thenReturn("테스트 상품2");

        ProductOption secondProductOption = mock(ProductOption.class);
        lenient().when(secondProductOption.getId()).thenReturn(20L);
        lenient().when(secondProductOption.getName()).thenReturn("추가 옵션");
        lenient().when(secondProductOption.getImageUrl()).thenReturn("image2.jpg");

        GroupBuy secondGroupBuy = mock(GroupBuy.class);
        lenient().when(secondGroupBuy.getProduct()).thenReturn(secondProduct);

        GroupBuyOption secondGroupBuyOption = mock(GroupBuyOption.class);
        lenient().when(secondGroupBuyOption.getId()).thenReturn(2L);
        lenient().when(secondGroupBuyOption.getGroupBuy()).thenReturn(secondGroupBuy);
        lenient().when(secondGroupBuyOption.getProductOption()).thenReturn(secondProductOption);
        lenient().when(secondGroupBuyOption.getSalePrice()).thenReturn(20000);

        OrderItem secondOrderItem = mock(OrderItem.class);
        lenient().when(secondOrderItem.getId()).thenReturn(2L);
        lenient().when(secondOrderItem.getGroupBuyOption()).thenReturn(secondGroupBuyOption);
        lenient().when(secondOrderItem.getQuantity()).thenReturn(2);

        RefundItem secondRefundItem = mock(RefundItem.class);
        lenient().when(secondRefundItem.getOrderItem()).thenReturn(secondOrderItem);

        return secondRefundItem;
    }

    private Refund createTestRefund() {
        Refund refund;
        try {
            Constructor<Refund> constructor = Refund.class.getDeclaredConstructor();
            constructor.setAccessible(true); // 접근 가능하게 변경
            refund = spy(constructor.newInstance()); // spy로 감싸기 (ReflectionTestUtils와 호환되게 하기 위함)
        } catch (Exception e) {
            throw new RuntimeException("Refund 인스턴스 생성 실패", e);
        }

        // 필드 값 설정
        ReflectionTestUtils.setField(refund, "id", REFUND_ID);
        ReflectionTestUtils.setField(refund, "createdAt", Instant.now());
        ReflectionTestUtils.setField(refund, "type", RefundType.CHANGE_OF_MIND);
        ReflectionTestUtils.setField(refund, "reason", "단순 변심");
        ReflectionTestUtils.setField(refund, "status", RefundStatus.APPROVED);
        ReflectionTestUtils.setField(refund, "rejectReason", null);
        ReflectionTestUtils.setField(refund, "refundedAt", null);
        ReflectionTestUtils.setField(refund, "amount", REFUND_AMOUNT);
        ReflectionTestUtils.setField(refund, "refundItems", List.of(testRefundItem));

        return refund;
    }
}