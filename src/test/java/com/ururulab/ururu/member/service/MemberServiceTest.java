package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.domain.repository.ShippingAddressRepository;
import com.ururulab.ururu.member.dto.request.MemberRequest;
import com.ururulab.ururu.member.dto.response.MemberGetResponse;
import com.ururulab.ururu.member.dto.response.MemberUpdateResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {
    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BeautyProfileRepository beautyProfileRepository;

    @Mock
    private ShippingAddressRepository shippingAddressRepository;

    @Mock
    private MemberAgreementRepository memberAgreementRepository;

    @Test
    @DisplayName("소셜 로그인 시 기존 회원 존재하면 해당 회원 반환")
    void findOrCreateMember_existingMember_returnsExistingMember() {
        //given
        SocialMemberInfo socialMemberInfo = MemberTestFixture.createSocialMemberInfo();
        Member existingMember = MemberTestFixture.createMember(1L, "기존사용자", "existing@example.com");

        given(memberRepository.findBySocialProviderAndSocialId(any(), any()))
                .willReturn(Optional.of(existingMember));

        // when
        Member result = memberService.findOrCreateMember(socialMemberInfo);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNickname()).isEqualTo("기존사용자");
        assertThat(result.getEmail()).isEqualTo("existing@example.com");

        then(memberRepository).should().findBySocialProviderAndSocialId(
                socialMemberInfo.provider(), socialMemberInfo.socialId());
        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("소셜 로그인 시 기존 회원이 없으면 새 회원을 생성")
    void findOrCreateMember_newMember() {
        //given
        SocialMemberInfo socialMemberInfo = MemberTestFixture.createSocialMemberInfo();
        Member newMember = MemberTestFixture.createMember(1L, "testuser", "test@example.com");

        given(memberRepository.findBySocialProviderAndSocialId(any(), any()))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willReturn(newMember);

        // when
        Member result = memberService.findOrCreateMember(socialMemberInfo);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNickname()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    void getMyProfile_validId_success() {
        // Given
        Long memberId = 1L;
        Member member = MemberTestFixture.createMember(memberId, "testuser", "test@example.com");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // When
        MemberGetResponse result = memberService.getMyProfile(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(memberId);
        assertThat(result.nickname()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 내 프로필 조회")
    void getMyProfile_invalidId_fail() {
        // Given
        Long invalidId = 999L;
        given(memberRepository.findById(invalidId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> memberService.getMyProfile(invalidId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("회원을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    void updateMyProfile_validRequest_success() {
        // Given
        Long memberId = 1L;
        Member existingMember = MemberTestFixture.createMember(memberId, "oldnick", "test@example.com");
        MemberRequest updateRequest = MemberTestFixture.createMemberUpdateRequest("newnick", "01099999999");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(existingMember));
        given(memberRepository.isNicknameAvailable("newnick")).willReturn(true);
        given(memberRepository.save(any(Member.class))).willReturn(existingMember);

        // When
        MemberUpdateResponse result = memberService.updateMyProfile(memberId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(memberId);

        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    @DisplayName("중복된 닉네임으로 프로필 수정")
    void updateMyProfile_duplicateNickname_fail() {
        // Given
        Long memberId = 1L;
        Member existingMember = MemberTestFixture.createMember(memberId, "oldnick", "test@example.com");
        MemberRequest updateRequest = MemberTestFixture.createMemberUpdateRequest("duplicateNick", "01099999999");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(existingMember));
        given(memberRepository.isNicknameAvailable("duplicateNick")).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberService.updateMyProfile(memberId, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }


}
