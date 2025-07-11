package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.dto.request.BeautyProfileRequest;
import com.ururulab.ururu.member.dto.response.BeautyProfileCreateResponse;
import com.ururulab.ururu.member.dto.response.BeautyProfileGetResponse;
import com.ururulab.ururu.member.dto.response.BeautyProfileUpdateResponse;
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
    void createBeautyProfile_memberNotFound_throwsException() {
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

    @Test
    @DisplayName("뷰티 프로필 조회 성공")
    void getBeautyProfile_success() {
        // Given
        Long memberId = 1L;
        Member member = BeautyProfileTestFixture.createMember(memberId);
        BeautyProfile beautyProfile = BeautyProfileTestFixture.createBeautyProfile(member);

        given(beautyProfileRepository.findByMemberId(memberId)).willReturn(Optional.of(beautyProfile));

        // When
        BeautyProfileGetResponse response = beautyProfileService.getBeautyProfile(memberId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.skinType().name()).isEqualTo("DRY");
        assertThat(response.skinTone().name()).isEqualTo("WARM");
        assertThat(response.concerns()).containsExactly("여드름", "건조함");
        assertThat(response.minPrice()).isEqualTo(10000);
        assertThat(response.maxPrice()).isEqualTo(50000);
    }

    @Test
    @DisplayName("뷰티 프로필 조회 실패 - 존재하지 않는 프로필")
    void getBeautyProfile_profileNotFound_throwsException() {
        // Given
        Long memberId = 999L;

        given(beautyProfileRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> beautyProfileService.getBeautyProfile(memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BEAUTY_PROFILE_NOT_FOUND);
    }

    @Test
    @DisplayName("뷰티 프로필 수정 성공")
    void updateBeautyProfile_success() {
        // Given
        Long memberId = 1L;
        Member member = BeautyProfileTestFixture.createMember(memberId);
        BeautyProfile existingProfile = BeautyProfileTestFixture.createBeautyProfile(member);
        BeautyProfileRequest updateRequest = BeautyProfileTestFixture.createUpdateRequest();

        given(beautyProfileRepository.findByMemberId(memberId)).willReturn(Optional.of(existingProfile));
        given(beautyProfileRepository.save(any(BeautyProfile.class))).willReturn(existingProfile);

        // When
        BeautyProfileUpdateResponse response = beautyProfileService.updateBeautyProfile(memberId, updateRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.skinType().name()).isEqualTo("OILY");
        assertThat(response.skinTone().name()).isEqualTo("COOL");
        assertThat(response.concerns()).containsExactly("모공", "과도한 유분");
        assertThat(response.hasAllergy()).isTrue();
        assertThat(response.allergies()).containsExactly("방부제");
    }

    @Test
    @DisplayName("뷰티 프로필 수정 실패 - 존재하지 않는 프로필")
    void updateBeautyProfile_profileNotFound_throwsException() {
        // Given
        Long memberId = 999L;
        BeautyProfileRequest request = BeautyProfileTestFixture.createValidRequest();

        given(beautyProfileRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> beautyProfileService.updateBeautyProfile(memberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BEAUTY_PROFILE_NOT_FOUND);
    }

    @Test
    @DisplayName("뷰티 프로필 삭제 성공")
    void deleteBeautyProfile_success() {
        // Given
        Long memberId = 1L;

        given(beautyProfileRepository.existsByMemberId(memberId)).willReturn(true);

        // When
        beautyProfileService.deleteBeautyProfile(memberId);

        // Then
        then(beautyProfileRepository).should().deleteByMemberId(memberId);
    }

    @Test
    @DisplayName("뷰티 프로필 삭제 실패 - 존재하지 않는 프로필")
    void deleteBeautyProfile_profileNotFound_throwsException() {
        // Given
        Long memberId = 999L;

        given(beautyProfileRepository.existsByMemberId(memberId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> beautyProfileService.deleteBeautyProfile(memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BEAUTY_PROFILE_NOT_FOUND);
    }

}
