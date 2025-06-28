package com.ururulab.ururu.member.service;

import com.ururulab.ururu.member.domain.dto.request.ShippingAddressRequest;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.domain.repository.ShippingAddressRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingAddressService {
    private final ShippingAddressRepository shippingAddressRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ShippingAddress createShippingAddress(Long memberId, ShippingAddressRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        int currentCount = shippingAddressRepository.countByMemberId(memberId);
        if (currentCount >= 5) {
            throw new IllegalArgumentException("배송지는 최대 5개까지 등록할 수 있습니다.");
        }

        if (request.isDefault()) {
            unsetExistingDefaultAddress(member);
        }

        ShippingAddress shippingAddress = ShippingAddress.of(
                member,
                request.label(),
                request.phone(),
                request.zonecode(),
                request.address1(),
                request.address2(),
                request.isDefault()
        );

        ShippingAddress savedAddress = shippingAddressRepository.save(shippingAddress);
        log.info("ShippingAddress created for member ID: {}", memberId);
        return savedAddress;
    }

    public List<ShippingAddress> getShippingAddresses(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));
        return shippingAddressRepository.findByMemberId(memberId);
    }

    public ShippingAddress getShippingAddressesById(Long memberId, Long addressId) {
        return shippingAddressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "배송지를 찾을 수 없습니다. Address ID: " + addressId +", Member ID: " + memberId));
    }

    @Transactional
    public ShippingAddress updateShippingAddress(
            Long memberId,
            Long addressId,
            ShippingAddressRequest request
    ) {
        ShippingAddress shippingAddress = shippingAddressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "배송지를 찾을 수 없습니다. Address ID: " + addressId));

        if (request.isDefault() && !shippingAddress.isDefault()) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "회원을 찾을 수 없습니다. ID: " + memberId));

            unsetExistingDefaultAddress(member);
        }

        shippingAddress.updateAddress(
                request.label(),
                request.phone(),
                request.zonecode(),
                request.address1(),
                request.address2(),
                request.isDefault()
        );

        ShippingAddress updatedAddress = shippingAddressRepository.save(shippingAddress);
        log.info("ShippingAddress updated for member ID: {}, address ID: {}", memberId, addressId);

        return updatedAddress;
    }

    @Transactional
    public void deleteShippingAddress(Long memberId, Long addressId) {
        boolean exists = shippingAddressRepository.existsByIdAndMemberId(addressId, memberId);
        if (!exists) {
            throw new EntityNotFoundException("배송지를 찾을 수 없습니다. Address ID: "+addressId);
        }

        shippingAddressRepository.deleteByIdAndMemberId(addressId, memberId);
        log.info("ShippingAddress deleted for member ID: {}, address ID: {}", memberId, addressId);
    }

    @Transactional
    public ShippingAddress setDefaultShippingAddress(Long memberId, Long addressId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        ShippingAddress shippingAddress = shippingAddressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "배송지를 찾을 수 없습니다. Address ID: " + addressId));

        unsetExistingDefaultAddress(member);
        shippingAddress.setAsDefault();

        ShippingAddress updatedAddress = shippingAddressRepository.save(shippingAddress);
        log.info("Default shipping address set for member ID: {}, address ID: {}", memberId, addressId);

        return updatedAddress;
    }

    @Transactional
    public void unsetDefaultShippingAddress(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        unsetExistingDefaultAddress(member);
    }

    private void unsetExistingDefaultAddress(Member member) {
        shippingAddressRepository.findByMemberAndIsDefaultTrue(member)
                .ifPresent(defaultAddress -> {
                    defaultAddress.unsetAsDefault();
                    shippingAddressRepository.save(defaultAddress);
                });
    }
}
