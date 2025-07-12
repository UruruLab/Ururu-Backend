package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.domain.repository.ShippingAddressRepository;
import com.ururulab.ururu.member.dto.request.ShippingAddressRequest;
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
public class ShippingAddressServiceTest {
    @InjectMocks
    private ShippingAddressService shippingAddressService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ShippingAddressRepository shippingAddressRepository;

    @Test
    @DisplayName("배송지 생성 성공")
    void createShippingAddress_success() {
        // Given
        Long memberId = 1L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddressRequest request = ShippingAddressTestFixture.createValidRequest();
        ShippingAddress savedAddress = ShippingAddressTestFixture.createShippingAddress(member);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(shippingAddressRepository.countByMemberId(memberId)).willReturn(2);
        given(shippingAddressRepository.save(any(ShippingAddress.class))).willReturn(savedAddress);

        // When
        ShippingAddress result = shippingAddressService.createShippingAddress(memberId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLabel()).isEqualTo("집");
        assertThat(result.getPhone()).isEqualTo("01012345678");
        assertThat(result.getZonecode()).isEqualTo("12345");
        assertThat(result.getAddress1()).isEqualTo("서울시 강남구 테헤란로 123");
        assertThat(result.getAddress2()).isEqualTo("456호");
        assertThat(result.isDefault()).isFalse();
    }

    @Test
    @DisplayName("배송지 생성 성공 - 기본 배송지로 생성 시 기존 기본 배송지 해제")
    void createShippingAddress_setAsDefault_unsetExistingDefault() {
        // Given
        Long memberId = 1L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress existingDefault = ShippingAddressTestFixture.createDefaultShippingAddress(member);
        ShippingAddressRequest request = ShippingAddressTestFixture.createDefaultRequest();
        ShippingAddress newDefaultAddress = ShippingAddressTestFixture.createNewDefaultShippingAddress(member);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(shippingAddressRepository.countByMemberId(memberId)).willReturn(1);
        given(shippingAddressRepository.findByMemberAndIsDefaultTrue(member))
                .willReturn(Optional.of(existingDefault));
        given(shippingAddressRepository.save(any(ShippingAddress.class)))
                .willReturn(existingDefault)  // 기존 기본 배송지 해제
                .willReturn(newDefaultAddress);  // 새 기본 배송지 저장

        // When
        ShippingAddress result = shippingAddressService.createShippingAddress(memberId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isTrue();

        // 기존 기본 배송지가 해제되었는지 확인
        then(shippingAddressRepository).should(times(2)).save(any(ShippingAddress.class));
        then(shippingAddressRepository).should().findByMemberAndIsDefaultTrue(member);
    }

    @Test
    @DisplayName("배송지 생성 실패 - 배송지 개수 제한 초과")
    void createShippingAddress_limitExceeded_throwsException() {
        // Given
        Long memberId = 1L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddressRequest request = ShippingAddressTestFixture.createValidRequest();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(shippingAddressRepository.countByMemberId(memberId)).willReturn(5);

        // When & Then
        assertThatThrownBy(() -> shippingAddressService.createShippingAddress(memberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHIPPING_ADDRESS_LIMIT_EXCEEDED);

        then(shippingAddressRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("배송지 목록 조회 성공")
    void getShippingAddresses_success() {
        // Given
        Long memberId = 1L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        List<ShippingAddress> addresses = ShippingAddressTestFixture.createShippingAddressList(member);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(shippingAddressRepository.findByMemberId(memberId)).willReturn(addresses);

        // When
        List<ShippingAddress> result = shippingAddressService.getShippingAddresses(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ShippingAddress::getLabel).containsExactly("집", "회사", "부모님집");
    }

    @Test
    @DisplayName("배송지 목록 조회 실패 - 존재하지 않는 회원")
    void getShippingAddresses_memberNotFound_throwsException() {
        // Given
        Long memberId = 999L;

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shippingAddressService.getShippingAddresses(memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_EXIST);
    }

    @Test
    @DisplayName("특정 배송지 조회")
    void getShippingAddressById_success() {
        // Given
        Long memberId = 1L;
        Long addressId = 100L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress address = ShippingAddressTestFixture.createShippingAddress(member);
        ShippingAddressRequest updatedRequest = ShippingAddressTestFixture.createValidRequest();

        given(shippingAddressRepository.findByIdAndMemberId(addressId, memberId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shippingAddressService.updateShippingAddress(memberId, addressId, updatedRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND);
        then(shippingAddressRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("기본 배송지 조회 성공")
    void getDefaultShippingAddress_success() {
        // Given
        Long memberId = 1L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress defaultAddress = ShippingAddressTestFixture.createDefaultShippingAddress(member);

        given(shippingAddressRepository.findByMemberIdAndIsDefaultTrue(memberId))
                .willReturn(Optional.of(defaultAddress));

        // When
        ShippingAddress result = shippingAddressService.getDefaultShippingAddress(memberId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    @DisplayName("기본 배송지 조회 실패 - 기본 배송지 없음")
    void getDefaultShippingAddress_notFound_throwsException() {
        // Given
        Long memberId = 1L;

        given(shippingAddressRepository.findByMemberIdAndIsDefaultTrue(memberId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shippingAddressService.getDefaultShippingAddress(memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEFAULT_SHIPPING_ADDRESS_NOT_FOUND);
    }

    @Test
    @DisplayName("배송지 수정 성공")
    void updateShippingAddress_success() {
        // Given
        Long memberId = 1L;
        Long addressId = 100L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress existingAddress = ShippingAddressTestFixture.createShippingAddress(member);
        ShippingAddressRequest updateRequest = ShippingAddressTestFixture.createUpdateRequest();

        given(shippingAddressRepository.findByIdAndMemberId(addressId, memberId))
                .willReturn(Optional.of(existingAddress));
        given(shippingAddressRepository.save(any(ShippingAddress.class))).willReturn(existingAddress);

        // When
        ShippingAddress result = shippingAddressService.updateShippingAddress(memberId, addressId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        then(shippingAddressRepository).should().save(existingAddress);
    }

    @Test
    @DisplayName("배송지 수정 성공 - 기본 배송지로 변경")
    void updateShippingAddress_setAsDefault_success() {
        // Given
        Long memberId = 1L;
        Long addressId = 100L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress targetAddress = ShippingAddressTestFixture.createShippingAddress(member);
        ShippingAddress existingDefault = ShippingAddressTestFixture.createDefaultShippingAddress(member);
        ShippingAddressRequest updateRequest = ShippingAddressTestFixture.createSetAsDefaultRequest();

        given(shippingAddressRepository.findByIdAndMemberId(addressId, memberId))
                .willReturn(Optional.of(targetAddress));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(shippingAddressRepository.findByMemberAndIsDefaultTrue(member))
                .willReturn(Optional.of(existingDefault));
        given(shippingAddressRepository.save(any(ShippingAddress.class))).willReturn(targetAddress);

        // When
        ShippingAddress result = shippingAddressService.updateShippingAddress(memberId, addressId, updateRequest);

        // Then
        assertThat(result).isNotNull();

        then(shippingAddressRepository).should().save(existingDefault);
        then(shippingAddressRepository).should().save(targetAddress);
        then(memberRepository).should().findById(memberId);
    }

    @Test
    @DisplayName("배송지 수정 성공 - 이미 기본 배송지인 경우 기존 기본 배송지 해제 로직 실행 안함")
    void updateShippingAddress_alreadyDefault_noUnsetLogic() {
        // Given
        Long memberId = 1L;
        Long addressId = 100L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress alreadyDefaultAddress = ShippingAddressTestFixture.createDefaultShippingAddress(member);
        ShippingAddressRequest updateRequest = ShippingAddressTestFixture.createSetAsDefaultRequest();

        given(shippingAddressRepository.findByIdAndMemberId(addressId, memberId))
                .willReturn(Optional.of(alreadyDefaultAddress));
        given(shippingAddressRepository.save(any(ShippingAddress.class))).willReturn(alreadyDefaultAddress);

        // When
        ShippingAddress result = shippingAddressService.updateShippingAddress(memberId, addressId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        then(memberRepository).should(never()).findById(memberId);
        then(shippingAddressRepository).should(never()).findByMemberAndIsDefaultTrue(any());
        then(shippingAddressRepository).should().save(alreadyDefaultAddress);
    }

    @Test
    @DisplayName("배송지 수정 실패 - 존재하지 않는 배송지")
    void updateShippingAddress_addressNotFound_throwsException() {
        // Given
        Long memberId = 1L;
        Long addressId = 999L;
        ShippingAddressRequest updateRequest = ShippingAddressTestFixture.createUpdateRequest();

        given(shippingAddressRepository.findByIdAndMemberId(addressId, memberId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shippingAddressService.updateShippingAddress(memberId, addressId, updateRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND);
    }

    @Test
    @DisplayName("기본 배송지 설정 성공")
    void setDefaultShippingAddress_success() {
        // Given
        Long memberId = 1L;
        Long addressId = 100L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress targetAddress = ShippingAddressTestFixture.createShippingAddress(member);
        ShippingAddress existingDefault = ShippingAddressTestFixture.createDefaultShippingAddress(member);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(shippingAddressRepository.findByIdAndMemberId(addressId, memberId))
                .willReturn(Optional.of(targetAddress));
        given(shippingAddressRepository.findByMemberAndIsDefaultTrue(member))
                .willReturn(Optional.of(existingDefault));
        given(shippingAddressRepository.save(any(ShippingAddress.class))).willReturn(targetAddress);

        // When
        ShippingAddress result = shippingAddressService.setDefaultShippingAddress(memberId, addressId);

        // Then
        assertThat(result).isNotNull();
        then(shippingAddressRepository).should().save(existingDefault);
        then(shippingAddressRepository).should().save(targetAddress);
    }

    @Test
    @DisplayName("기본 배송지 설정 성공 - 기존 기본 배송지가 없는 경우")
    void setDefaultShippingAddress_noExistingDefault_success() {
        // Given
        Long memberId = 1L;
        Long addressId = 100L;
        Member member = ShippingAddressTestFixture.createMember(memberId);
        ShippingAddress targetAddress = ShippingAddressTestFixture.createShippingAddress(member);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(shippingAddressRepository.findByIdAndMemberId(addressId, memberId))
                .willReturn(Optional.of(targetAddress));
        given(shippingAddressRepository.findByMemberAndIsDefaultTrue(member))
                .willReturn(Optional.empty()); // 기존 기본 배송지 없음
        given(shippingAddressRepository.save(any(ShippingAddress.class))).willReturn(targetAddress);

        // When
        ShippingAddress result = shippingAddressService.setDefaultShippingAddress(memberId, addressId);

        // Then
        assertThat(result).isNotNull();

        // 기존 기본 배송지가 없으므로 해제 로직은 실행되지 않고, 새 기본 배송지만 설정
        then(shippingAddressRepository).should().findByMemberAndIsDefaultTrue(member);
        then(shippingAddressRepository).should().save(targetAddress); // 새 기본 배송지 설정
    }

    @Test
    @DisplayName("기본 배송지 설정 실패 - 존재하지 않는 회원")
    void setDefaultShippingAddress_memberNotFound_throwsException() {
        // Given
        Long memberId = 999L;
        Long addressId = 100L;

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shippingAddressService.setDefaultShippingAddress(memberId, addressId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_EXIST);

        then(shippingAddressRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("배송지 삭제 성공")
    void deleteShippingAddress_success() {
        // Given
        Long memberId = 1L;
        Long addressId = 100L;

        given(shippingAddressRepository.existsByIdAndMemberId(addressId, memberId)).willReturn(true);

        // When
        shippingAddressService.deleteShippingAddress(memberId, addressId);

        // Then
        then(shippingAddressRepository).should().deleteByIdAndMemberId(addressId, memberId);
    }

    @Test
    @DisplayName("배송지 삭제 실패 - 존재하지 않는 배송지")
    void deleteShippingAddress_addressNotFound_throwsException() {
        // Given
        Long memberId = 1L;
        Long addressId = 999L;

        given(shippingAddressRepository.existsByIdAndMemberId(addressId, memberId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> shippingAddressService.deleteShippingAddress(memberId, addressId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND);

        then(shippingAddressRepository).should(never()).deleteByIdAndMemberId(any(), any());
    }
}
