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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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
}
