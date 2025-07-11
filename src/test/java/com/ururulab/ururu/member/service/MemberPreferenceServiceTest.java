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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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
    void createPreference_SellerNotFound_ThrowsException() {
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
    void createPreference_AlreadyExists_ThrowsException() {
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
}
