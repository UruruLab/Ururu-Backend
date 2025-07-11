package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberPreference;
import com.ururulab.ururu.member.domain.repository.MemberPreferenceRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.dto.request.MemberPreferenceRequest;
import com.ururulab.ururu.member.dto.response.MemberPreferenceResponse;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberPreferenceServiceTest {
    @InjectMocks
    private MemberPreferenceService memberPreferenceService;

    @Mock
    private MemberPreferenceRepository memberPreferenceRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Test
    @DisplayName("회원 선호도 생성 성공")
    void createPreference_success() {
        // Given
        Long memberId = 1L;
        Long sellerId = 100L;
        Member member = MemberPreferenceTestFixture.createMember(memberId);
        MemberPreferenceRequest request = MemberPreferenceTestFixture.createValidRequest(sellerId);
        MemberPreference savedPreference = MemberPreferenceTestFixture.createMemberPreference(member, sellerId);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(sellerRepository.existsByIdAndIsDeletedFalse(sellerId)).willReturn(true);
        given(memberPreferenceRepository.existsByMemberIdAndSellerId(memberId, sellerId)).willReturn(false);
        given(memberPreferenceRepository.save(any(MemberPreference.class))).willReturn(savedPreference);

        // When
        MemberPreferenceResponse response = memberPreferenceService.createPreference(memberId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.sellerId()).isEqualTo(sellerId);
        assertThat(response.preferenceLevel()).isEqualTo(4);
        assertThat(response.monthlyBudget()).isEqualTo(50000);
        assertThat(response.purchaseFrequency().name()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("회원 선호도 생성 실패 - 존재하지 않는 판매자")
    void createPreference_sellerNotFound_throwsException() {
        // Given
        Long memberId = 1L;
        Long sellerId = 999L;
        Member member = MemberPreferenceTestFixture.createMember(memberId);
        MemberPreferenceRequest request = MemberPreferenceTestFixture.createValidRequest(sellerId);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(sellerRepository.existsByIdAndIsDeletedFalse(sellerId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberPreferenceService.createPreference(memberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SELLER_NOT_FOUND);

        then(memberPreferenceRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("회원 선호도 생성 실패 - 이미 존재하는 선호도")
    void createPreference_alreadyExists_throwsException() {
        // Given
        Long memberId = 1L;
        Long sellerId = 100L;
        Member member = MemberPreferenceTestFixture.createMember(memberId);
        MemberPreferenceRequest request = MemberPreferenceTestFixture.createValidRequest(sellerId);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(sellerRepository.existsByIdAndIsDeletedFalse(sellerId)).willReturn(true);
        given(memberPreferenceRepository.existsByMemberIdAndSellerId(memberId, sellerId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> memberPreferenceService.createPreference(memberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_PREFERENCE_ALREADY_EXISTS);

        then(memberPreferenceRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("회원 선호도 목록 조회 성공")
    void getMemberPreference_success() {
        // Given
        Long memberId = 1L;
        List<MemberPreference> preferences = MemberPreferenceTestFixture.createMemberPreferenceList(memberId);

        given(memberRepository.existsById(memberId)).willReturn(true);
        given(memberPreferenceRepository.findByMemberId(memberId)).willReturn(preferences);

        // When
        List<MemberPreferenceResponse> responses = memberPreferenceService.getMemberPreferences(memberId);

        // Then
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(MemberPreferenceResponse::sellerId)
                .containsExactly(100L, 101L, 102L);
        assertThat(responses).extracting(MemberPreferenceResponse::preferenceLevel)
                .containsExactly(4,5,3);
    }

    @Test
    @DisplayName("회원 선호도 목록 조회 실패 - 존재하지 않는 회원")
    void getMemberPreferences_memberNotFound_throwsException() {
        // Given
        Long memberId = 999L;

        given(memberRepository.existsById(memberId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberPreferenceService.getMemberPreferences(memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_EXIST);

        then(memberPreferenceRepository).should(never()).findByMemberId(any());
    }
}
