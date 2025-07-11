package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.domain.repository.ShippingAddressRepository;
import com.ururulab.ururu.member.dto.request.MemberRequest;
import com.ururulab.ururu.member.dto.response.*;
import com.ururulab.ururu.order.domain.repository.CartItemRepository;
import com.ururulab.ururu.order.domain.repository.CartRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
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

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Test
    @DisplayName("소셜 로그인 시 기존 회원 존재하면 해당 회원 반환")
    void findOrCreateMember_existingMember_returnsExistingMember() {
        // Given
        SocialMemberInfo socialMemberInfo = MemberTestFixture.createSocialMemberInfo();
        Member existingMember = MemberTestFixture.createMember(1L, "기존사용자", "existing@example.com");

        given(memberRepository.findBySocialProviderAndSocialId(any(), any()))
                .willReturn(Optional.of(existingMember));

        // When
        Member result = memberService.findOrCreateMember(socialMemberInfo);

        // Then
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
        // Given
        SocialMemberInfo socialMemberInfo = MemberTestFixture.createSocialMemberInfo();
        Member newMember = MemberTestFixture.createMember(1L, "testuser", "test@example.com");

        given(memberRepository.findBySocialProviderAndSocialId(any(), any()))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willReturn(newMember);

        // When
        Member result = memberService.findOrCreateMember(socialMemberInfo);

        // Then
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

    // TODO: uploadProfileImage(), deleteProfileImage Test code 추후 작성
    // (아직 실제 로직 구현하지 않음)

    @Test
    @DisplayName("닉네임 존재 여부 확인 - 존재하는 경우")
    void checkNicknameExists_existingNickname() {
        // Given
        String existingNickname = "exist";
        given(memberRepository.existsByNickname(existingNickname)).willReturn(true);

        // When
        boolean result = memberService.checkNicknameExists(existingNickname);

        // Then
        assertThat(result).isTrue();
        then(memberRepository).should().existsByNickname(existingNickname);
    }

    @Test
    @DisplayName("닉네임 사용 가능 여부 확인 - 사용가능한 경우")
    void getNicknameAvailability_availableNickname() {
        // Given
        String availableNickname = "available";
        given(memberRepository.isNicknameAvailable(availableNickname)).willReturn(true);

        // When
        NicknameAvailabilityResponse result = memberService.getNicknameAvailability(availableNickname);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAvailable()).isTrue();
        then(memberRepository).should().isNicknameAvailable(availableNickname);
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 - 존재하는 경우")
    void checkEmailExists_existingEmail() {
        // Given
        String existingEmail = "existing@example.com";
        given(memberRepository.existsByEmail(existingEmail)).willReturn(true);

        // When
        boolean result = memberService.checkEmailExists(existingEmail);

        // Then
        assertThat(result).isTrue();
        then(memberRepository).should().existsByEmail(existingEmail);
    }

    @Test
    @DisplayName("이메일 사용 가능 여부 확인 - 사용가능한 경우")
    void getEmailAvailable_availableEmail() {
        // Given
        String availableEmail = "available@example.com";
        given(memberRepository.isEmailAvailable(availableEmail)).willReturn(true);

        // When
        EmailAvailabilityResponse result = memberService.getEmailAvailability(availableEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAvailable()).isTrue();
        then(memberRepository).should().isEmailAvailable(availableEmail);
    }

    @Test
    @DisplayName("회원 삭제 성공")
    void deleteMember_validMemberId_success() {
        // Given
        Long memberId = 1L;
        Member member = MemberTestFixture.createMember(memberId, "testuser", "test@example.com");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willReturn(member);

        given(orderRepository.countActiveOrdersByMemberId(memberId)).willReturn(0);
        given(paymentRepository.existsPendingPaymentsByMemberId(memberId)).willReturn(false);

        given(cartRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        // When
        memberService.deleteMember(memberId);

        // Then
        then(orderRepository).should().countActiveOrdersByMemberId(memberId);
        then(paymentRepository).should().existsPendingPaymentsByMemberId(memberId);

        then(memberAgreementRepository).should().deleteByMemberId(memberId);
        then(shippingAddressRepository).should().deleteByMemberId(memberId);
        then(beautyProfileRepository).should().deleteByMemberId(memberId);
        then(cartRepository).should().findByMemberId(memberId);

        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    @DisplayName("회원 삭제 - 활성 주문이 있는 회원 삭제 시 실패")
    void deleteMember_hasActiveOrders_fail() {
        // Given
        Long memberId = 1L;
        Member member = MemberTestFixture.createMember(memberId, "testuser", "test@example.com");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(orderRepository.countActiveOrdersByMemberId(memberId)).willReturn(2);

        // When & Then
        assertThatThrownBy(() -> memberService.deleteMember(memberId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행 중인 주문이 2건 있어 탈퇴할 수 없습니다.");

        then(shippingAddressRepository).should(never()).deleteByMemberId(any());
        then(beautyProfileRepository).should(never()).deleteByMemberId(any());
        then(memberAgreementRepository).should(never()).deleteByMemberId(any());
        then(memberRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("회원 탈퇴 미리보기를 정상적으로 조회한다")
    void getWithdrawalPreview_validMember_returnsPreview() {
        // Given
        Long memberId = 1L;
        Member member = MemberTestFixture.createMember(memberId, "testuser", "test@example.com");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        given(orderRepository.countActiveOrdersByMemberId(memberId)).willReturn(1);
        given(beautyProfileRepository.existsByMemberId(memberId)).willReturn(true);
        given(shippingAddressRepository.countByMemberId(memberId)).willReturn(2);
        given(memberAgreementRepository.countByMemberId(memberId)).willReturn(4);
        given(cartItemRepository.countByCartMemberId(memberId)).willReturn(3);
        given(pointTransactionRepository.countByMemberId(memberId)).willReturn(5);

        // When
        WithdrawalPreviewResponse result = memberService.getWithdrawalPreview(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.memberInfo().nickname()).isEqualTo("testuser");
        assertThat(result.memberInfo().email()).isEqualTo("test@example.com");
        assertThat(result.lossInfo().activeOrders()).isEqualTo(1);
        assertThat(result.lossInfo().beautyProfileExists()).isTrue();
        assertThat(result.lossInfo().shippingAddressesCount()).isEqualTo(2);
        assertThat(result.lossInfo().memberAgreementsCount()).isEqualTo(4);
        assertThat(result.lossInfo().cartItemsCount()).isEqualTo(3);
        assertThat(result.lossInfo().pointTransactionsCount()).isEqualTo(5);
    }
}
