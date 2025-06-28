package com.ururulab.ururu.member.domain.dto.request;

import com.ururulab.ururu.member.domain.dto.validation.MemberAgreementValidationMessages;
import com.ururulab.ururu.member.domain.entity.enumerated.AgreementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemberAgreementRequest(
        @NotEmpty(message = MemberAgreementValidationMessages.AGREEMENTS_REQUIRED)
        List<@Valid AgreementItem> agreements
) {
    public record AgreementItem(
            @NotNull(message = MemberAgreementValidationMessages.AGREEMENT_TYPE_REQUIRED)
            AgreementType type,

            @NotNull(message = MemberAgreementValidationMessages.AGREEMENT_AGREED_REQUIRED)
            Boolean agreed
    ) {
        public static AgreementItem of(final AgreementType type, final Boolean agreed) {
            return new AgreementItem(type, agreed);
        }
    }
}
