package com.ururulab.ururu.member.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberAgreementValidationMessages {
    public static final String AGREEMENTS_REQUIRED = "약관 동의 내역은 최소 " +
            MemberAgreementValidationConstants.MIN_AGREEMENTS_COUNT + "개 이상이어야 합니다.";

    public static final String AGREEMENT_TYPE_REQUIRED = "약관 타입은 필수입니다.";
    public static final String AGREEMENT_AGREED_REQUIRED = "동의 여부는 필수입니다.";

}
