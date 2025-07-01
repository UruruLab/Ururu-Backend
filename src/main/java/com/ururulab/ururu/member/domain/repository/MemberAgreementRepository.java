package com.ururulab.ururu.member.domain.repository;

import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {
    int countByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}
