package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.domain.repository.ShippingAddressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void findOrCreateMember_ExistingMember_ReturnsExistingMember() {
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
    void findOrCreateMember_NewMember() {
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



}
