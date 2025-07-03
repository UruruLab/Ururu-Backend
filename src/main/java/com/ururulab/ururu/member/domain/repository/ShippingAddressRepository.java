package com.ururulab.ururu.member.domain.repository;

import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findByMemberId(Long memberId);
    Optional<ShippingAddress> findByMemberAndIsDefaultTrue(Member member);
    Optional<ShippingAddress> findByMemberIdAndIsDefaultTrue(Long memberId);
    Optional<ShippingAddress> findByIdAndMemberId(Long id, Long memberId);
    int countByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
    boolean existsByIdAndMemberId(Long addressId, Long memberId);
    void deleteByIdAndMemberId(Long addressId, Long memberId);
}
