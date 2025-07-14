package com.ururulab.ururu.member.domain.repository;

import com.ururulab.ururu.member.domain.entity.MemberPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberPreferenceRepository extends JpaRepository<MemberPreference, Long> {
    List<MemberPreference> findByMemberId(Long memberId);
    boolean existsByMemberIdAndSellerId(Long memberId, Long sellerId);
    void deleteByMemberId(Long memberId);
}
