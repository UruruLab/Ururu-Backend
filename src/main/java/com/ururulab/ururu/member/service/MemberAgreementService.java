package com.ururulab.ururu.member.service;

import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import com.ururulab.ururu.member.domain.entity.enumerated.AgreementType;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAgreementService {
    private final MemberAgreementRepository memberAgreementRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createAgreements(Long memberId, Map<AgreementType, Boolean> agreements) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        List<MemberAgreement> memberAgreements = agreements.entrySet().stream()
                .map(entry -> MemberAgreement.of(member, entry.getKey(), entry.getValue()))
                .toList();

        memberAgreementRepository.saveAll(memberAgreements);
        log.info("Member agreements created for member ID: {}", memberId);
    }
}
