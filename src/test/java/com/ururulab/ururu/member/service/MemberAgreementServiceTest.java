package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.dto.request.MemberAgreementRequest;
import com.ururulab.ururu.member.dto.response.MemberAgreementCreateResponse;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberAgreementServiceTest {
    @InjectMocks
    private MemberAgreementService memberAgreementService;

    @Mock
    private MemberAgreementRepository memberAgreementRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 약관 동의 생성 성공 - 모든 약관 동의")
    void createAgreements_success() {
        // Given
        Long memberId = 1L;
        Member member = MemberAgreementTestFixture.createMember(memberId);
        MemberAgreementRequest request = MemberAgreementTestFixture.createAllAgreedRequest();
        List<MemberAgreement> savedAgreements = MemberAgreementTestFixture.createAllAgreedMemberAgreements(member);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberAgreementRepository.saveAll(anyList())).willReturn(savedAgreements);

        // When
        MemberAgreementCreateResponse response = memberAgreementService.createAgreements(memberId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.agreements()).hasSize(4);
        assertThat(response.agreements())
                .extracting(item -> item.agreed())
                .containsOnly(true);
    }

    @Test
    @DisplayName("회원 약관 동의 생성 성공 - 필수 약관만 동의")
    void createAgreements_RequiredOnly_Success() {
        // Given
        Long memberId = 1L;
        Member member = MemberAgreementTestFixture.createMember(memberId);
        MemberAgreementRequest request = MemberAgreementTestFixture.createRequiredOnlyRequest();
        List<MemberAgreement> savedAgreements = MemberAgreementTestFixture.createRequiredOnlyMemberAgreements(member);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberAgreementRepository.saveAll(anyList())).willReturn(savedAgreements);

        // When
        MemberAgreementCreateResponse response = memberAgreementService.createAgreements(memberId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.agreements()).hasSize(2);

        // 필수 약관만 동의되어 있는지 확인
        assertThat(response.agreements())
                .extracting(item -> item.type().name())
                .containsExactlyInAnyOrder("TERMS_OF_SERVICE", "PRIVACY_POLICY");

        assertThat(response.agreements())
                .extracting(MemberAgreementCreateResponse.AgreementItem::agreed)
                .containsOnly(true);
    }

    @Test
    @DisplayName("회원 약관 동의 생성 실패 - 존재하지 않는 회원")
    void createAgreements_MemberNotFound_ThrowsException() {
        // Given
        Long memberId = 999L;
        MemberAgreementRequest request = MemberAgreementTestFixture.createValidAgreementRequest();

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberAgreementService.createAgreements(memberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_EXIST);

        then(memberAgreementRepository).should(never()).saveAll(anyList());
    }
}
