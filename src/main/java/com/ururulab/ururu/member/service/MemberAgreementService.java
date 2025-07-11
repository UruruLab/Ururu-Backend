package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.dto.request.MemberAgreementRequest;
import com.ururulab.ururu.member.dto.response.MemberAgreementCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAgreementService {
    private final MemberAgreementRepository memberAgreementRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberAgreementCreateResponse createAgreements(Long memberId, MemberAgreementRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));

        List<MemberAgreement> memberAgreements = request.agreements().stream()
                .map(agreementItem -> MemberAgreement.of(
                        member,
                        agreementItem.type(),
                        agreementItem.agreed()))
                .toList();

        List<MemberAgreement> savedAgreements = memberAgreementRepository.saveAll(memberAgreements);
        log.debug("Member agreements created for member ID: {}", memberId);
        return MemberAgreementCreateResponse.of(memberId, savedAgreements);
    }
}
