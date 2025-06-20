package com.ururulab.ururu.user.domain.repository;

import com.ururulab.ururu.user.domain.entity.Member;
import com.ururulab.ururu.user.domain.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findByMember(Member member);
    List<ShippingAddress> findByMemberId(Long memberId);
    Optional<ShippingAddress> findByMemberAndIsDefaultTrue(Member member);
    Optional<ShippingAddress> findByMemberIdAndIsDefaultTrue(Long memberId);
    Optional<ShippingAddress> findByIdAndMemberId(Long id, Long memberId);

}
