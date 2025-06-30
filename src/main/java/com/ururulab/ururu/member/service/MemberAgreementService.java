package com.ururulab.ururu.member.service;

import com.ururulab.ururu.member.domain.dto.request.MemberAgreementRequest;
import com.ururulab.ururu.member.domain.dto.response.MemberAgreementCreateResponse;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
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
public class MemberAgreementService {
    private final MemberAgreementRepository memberAgreementRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberAgreementCreateResponse createAgreements(Long memberId, MemberAgreementRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        List<MemberAgreement> memberAgreements = request.agreements().stream()
                .map(agreementItem -> MemberAgreement.of(
                        member,
                        agreementItem.type(),
                        agreementItem.agreed()))
                .toList();

        List<MemberAgreement> savedAgreements = memberAgreementRepository.saveAll(memberAgreements);
        log.info("Member agreements created for member ID: {}", memberId);
        return MemberAgreementCreateResponse.of(memberId, savedAgreements);
    }
}
