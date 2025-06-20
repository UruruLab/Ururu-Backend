package com.ururulab.ururu.user.domain.repository;

import com.ururulab.ururu.user.domain.entity.Member;
import com.ururulab.ururu.user.domain.entity.MemberPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberPreferenceRepository extends JpaRepository<MemberPreference, Long> {
    List<MemberPreference> findByMember(Member member);
    List<MemberPreference> findByMemberId(Long memberId);
}
