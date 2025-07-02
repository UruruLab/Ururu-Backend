package com.ururulab.ururu.member.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import com.ururulab.ururu.member.domain.entity.enumerated.AgreementType;

import java.time.Instant;
import java.util.List;

public record MemberAgreementCreateResponse(
        @JsonProperty("member_id") Long memberId,
        List<AgreementItem> agreements,
        @JsonProperty("created_at") Instant createdAt
) {
    public static MemberAgreementCreateResponse of(final Long memberId, final List<MemberAgreement> memberAgreements) {
        final List<AgreementItem> agreements = memberAgreements.stream()
                .map(AgreementItem::from)
                .toList();

        final Instant createdAt = memberAgreements.stream()
                .findFirst()
                .map(MemberAgreement::getCreatedAt)
                .orElse(Instant.now());

        return new MemberAgreementCreateResponse(memberId, agreements, createdAt);
    }

    public record AgreementItem(
            Long id,
            AgreementType type,
            Boolean agreed,
            @JsonProperty("agree_at") Instant agreeAt
    ) {
        public static AgreementItem from(final MemberAgreement memberAgreement) {
            return new AgreementItem(
                    memberAgreement.getId(),
                    memberAgreement.getType(),
                    memberAgreement.isAgreed(),
                    memberAgreement.getAgreeAt() != null ? memberAgreement.getAgreeAt().toInstant() : null
            );
        }
    }
}
