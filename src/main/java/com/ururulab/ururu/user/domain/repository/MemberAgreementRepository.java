package com.ururulab.ururu.user.domain.repository;

import com.ururulab.ururu.user.domain.entity.Member;
import com.ururulab.ururu.user.domain.entity.MemberAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {
    List<MemberAgreement> findByMember(Member member);
    List<MemberAgreement> findByMemberId(Long memberId);
}
