package com.ururulab.ururu.member.domain.repository;

import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {
    List<MemberAgreement> findByMember(Member member);
    List<MemberAgreement> findByMemberId(Long memberId);
    int countByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}
