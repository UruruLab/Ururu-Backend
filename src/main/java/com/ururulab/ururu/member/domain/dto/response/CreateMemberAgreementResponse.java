package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import com.ururulab.ururu.member.domain.entity.enumerated.AgreementType;

import java.time.ZonedDateTime;
import java.util.List;

public record CreateMemberAgreementResponse(
        @JsonProperty("member_id") Long memberId,
        List<AgreementItem> agreements,
        @JsonProperty("created_at") ZonedDateTime createdAt
) {
    public static CreateMemberAgreementResponse of(final Long memberId, final List<MemberAgreement> memberAgreements) {
        final List<AgreementItem> agreements = memberAgreements.stream()
                .map(AgreementItem::from)
                .toList();

        final ZonedDateTime createdAt = memberAgreements.stream()
                .findFirst()
                .map(MemberAgreement::getCreatedAt)
                .orElse(ZonedDateTime.now());

        return new CreateMemberAgreementResponse(memberId, agreements, createdAt);
    }

    public record AgreementItem(
            Long id,
            AgreementType type,
            Boolean agreed,
            @JsonProperty("agree_at") ZonedDateTime agreeAt
    ) {
        public static AgreementItem from(final MemberAgreement memberAgreement) {
            return new AgreementItem(
                    memberAgreement.getId(),
                    memberAgreement.getType(),
                    memberAgreement.isAgreed(),
                    memberAgreement.getAgreeAt()
            );
        }
    }
}
