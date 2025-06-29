package com.ururulab.ururu.member.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberPreferenceValidationMessages {
    public static final String SELLER_ID_REQUIRED = "판매자 ID는 필수입니다.";

    public static final String PREFERENCE_LEVEL_REQUIRED = "선호도 레벨은 필수입니다.";
    public static final String PREFERENCE_LEVEL_MIN = "선호도 레벨은 " +
            MemberPreferenceValidationConstants.PREFERENCE_LEVEL_MIN + " 이상이어야 합니다.";
    public static final String PREFERENCE_LEVEL_MAX = "선호도 레벨은 " +
            MemberPreferenceValidationConstants.PREFERENCE_LEVEL_MAX + " 이하여야 합니다.";

    public static final String MONTHLY_BUDGET_REQUIRED = "월 예산은 필수입니다.";
    public static final String MONTHLY_BUDGET_MIN = "월 예산은 " +
            MemberPreferenceValidationConstants.MONTHLY_BUDGET_MIN + "원 이상이어야 합니다.";

    public static final String PURCHASE_FREQUENCY_REQUIRED = "구매 빈도는 필수입니다.";
    public static final String PURCHASE_FREQUENCY_INVALID = "올바른 구매 빈도 값이 아닙니다.";
}
