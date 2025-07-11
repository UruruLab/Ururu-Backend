package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.dto.request.BeautyProfileRequest;
import com.ururulab.ururu.member.dto.response.BeautyProfileCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class BeautyProfileServiceTest {
    @InjectMocks
    private BeautyProfileService beautyProfileService;

    @Mock
    private BeautyProfileRepository beautyProfileRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("뷰티 프로필 생성 성공")
    void createBeautyProfile_success() {
        // Given
        Long memberId = 1L;
        Member member = BeautyProfileTestFixture.createMember(memberId);
        BeautyProfileRequest request = BeautyProfileTestFixture.createValidRequest();
        BeautyProfile savedProfile = BeautyProfileTestFixture.createBeautyProfile(member);

        given(beautyProfileRepository.existsByMemberId(memberId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(beautyProfileRepository.save(any(BeautyProfile.class))).willReturn(savedProfile);

        // Will
        BeautyProfileCreateResponse response = beautyProfileService.createBeautyProfile(memberId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.skinType().name()).isEqualTo("DRY");
        assertThat(response.skinTone().name()).isEqualTo("WARM");
        assertThat(response.concerns()).containsExactly("여드름", "건조함");
        assertThat(response.hasAllergy()).isFalse();
        assertThat(response.minPrice()).isEqualTo(10000);
        assertThat(response.maxPrice()).isEqualTo(50000);
    }

    @Test
    @DisplayName("뷰티 프로필 생성 실패 - 존재하지 않는 회원")
    void createBeautyProfile_MemberNotFound_ThrowsException() {
        // Given
        Long memberId = 999L;
        BeautyProfileRequest request = BeautyProfileTestFixture.createValidRequest();

        given(beautyProfileRepository.existsByMemberId(memberId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> beautyProfileService.createBeautyProfile(memberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_EXIST);
    }
}
