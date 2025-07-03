package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.payment.dto.response.MemberPointResponse;
import com.ururulab.ururu.payment.dto.response.PointTransactionListResponse;
import com.ururulab.ururu.payment.domain.entity.PointTransaction;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointType;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 테스트")
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    private static final Long MEMBER_ID = 1L;
    private static final Integer MEMBER_POINTS = 5000;

    private Member testMember;
    private PointTransaction testTransaction;

    @BeforeEach
    void setUp() {
        testMember = createTestMember();
        testTransaction = createTestTransaction();
    }

    @Nested
    @DisplayName("현재 포인트 조회")
    class GetCurrentPointsTest {

        @Test
        @DisplayName("성공")
        void getCurrentPoints_success() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(testMember));

            // when
            MemberPointResponse result = pointService.getCurrentPoints(MEMBER_ID);

            // then
            assertThat(result.currentPoints()).isEqualTo(MEMBER_POINTS);
            verify(memberRepository).findById(MEMBER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 회원")
        void getCurrentPoints_memberNotFound_fail() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> pointService.getCurrentPoints(MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

            verify(memberRepository).findById(MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("포인트 거래내역 조회")
    class GetPointTransactionsTest {

        @Test
        @DisplayName("성공 - 전체 조회")
        void getPointTransactions_all_success() {
            // given
            Page<PointTransaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
            given(pointTransactionRepository.findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(transactionPage);

            // when
            PointTransactionListResponse result = pointService.getPointTransactions(
                    MEMBER_ID, "all", "all", 1, 10);

            // then
            assertThat(result.transactions()).hasSize(1);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.totalPages()).isEqualTo(1);

            assertThat(result.transactions().get(0).id()).isEqualTo(1L);
            assertThat(result.transactions().get(0).type()).isEqualTo(PointType.USED);
            assertThat(result.transactions().get(0).source()).isEqualTo(PointSource.GROUPBUY);
            assertThat(result.transactions().get(0).amount()).isEqualTo(2000);

            verify(memberRepository).findById(MEMBER_ID);
            verify(pointTransactionRepository).findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 타입 필터링")
        void getPointTransactions_typeFilter_success() {
            // given
            Page<PointTransaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
            given(pointTransactionRepository.findByMemberIdWithFilters(
                    eq(MEMBER_ID), eq(PointType.USED), isNull(), any(Pageable.class)))
                    .willReturn(transactionPage);

            // when
            PointTransactionListResponse result = pointService.getPointTransactions(
                    MEMBER_ID, "USED", "all", 1, 10);

            // then
            assertThat(result.transactions()).hasSize(1);
            verify(pointTransactionRepository).findByMemberIdWithFilters(
                    eq(MEMBER_ID), eq(PointType.USED), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 소스 필터링")
        void getPointTransactions_sourceFilter_success() {
            // given
            Page<PointTransaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
            given(pointTransactionRepository.findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), eq(PointSource.GROUPBUY), any(Pageable.class)))
                    .willReturn(transactionPage);

            // when
            PointTransactionListResponse result = pointService.getPointTransactions(
                    MEMBER_ID, "all", "GROUPBUY", 1, 10);

            // then
            assertThat(result.transactions()).hasSize(1);
            verify(pointTransactionRepository).findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), eq(PointSource.GROUPBUY), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 잘못된 타입 파라미터는 all로 처리")
        void getPointTransactions_invalidType_treatedAsAll() {
            // given
            Page<PointTransaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
            given(pointTransactionRepository.findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(transactionPage);

            // when
            PointTransactionListResponse result = pointService.getPointTransactions(
                    MEMBER_ID, "INVALID_TYPE", "all", 1, 10);

            // then
            assertThat(result.transactions()).hasSize(1);
            verify(pointTransactionRepository).findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 잘못된 소스 파라미터는 all로 처리")
        void getPointTransactions_invalidSource_treatedAsAll() {
            // given
            Page<PointTransaction> transactionPage = new PageImpl<>(List.of(testTransaction));

            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
            given(pointTransactionRepository.findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(transactionPage);

            // when
            PointTransactionListResponse result = pointService.getPointTransactions(
                    MEMBER_ID, "all", "INVALID_SOURCE", 1, 10);

            // then
            assertThat(result.transactions()).hasSize(1);
            verify(pointTransactionRepository).findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원")
        void getPointTransactions_memberNotFound_fail() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> pointService.getPointTransactions(
                    MEMBER_ID, "all", "all", 1, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

            verify(memberRepository).findById(MEMBER_ID);
            verify(pointTransactionRepository, never()).findByMemberIdWithFilters(
                    anyLong(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        void getPointTransactions_emptyResult_success() {
            // given
            Page<PointTransaction> emptyPage = new PageImpl<>(List.of());

            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(testMember));
            given(pointTransactionRepository.findByMemberIdWithFilters(
                    eq(MEMBER_ID), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when
            PointTransactionListResponse result = pointService.getPointTransactions(
                    MEMBER_ID, "all", "all", 1, 10);

            // then
            assertThat(result.transactions()).isEmpty();
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.total()).isEqualTo(0L);
            assertThat(result.totalPages()).isEqualTo(1); // 빈 페이지도 totalPages는 1
        }
    }

    private Member createTestMember() {
        Member member = Member.of(
                "테스트유저", "test@example.com", SocialProvider.KAKAO,
                "social123", Gender.NONE, null, null, null, Role.NORMAL
        );
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        ReflectionTestUtils.setField(member, "point", MEMBER_POINTS);
        return member;
    }

    private PointTransaction createTestTransaction() {
        PointTransaction transaction = PointTransaction.createUsed(
                testMember, PointSource.GROUPBUY, 2000, "공동구매 결제"
        );
        ReflectionTestUtils.setField(transaction, "id", 1L);
        ReflectionTestUtils.setField(transaction, "createdAt", Instant.now());
        return transaction;
    }
}