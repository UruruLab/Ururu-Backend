package com.ururulab.ururu.member.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BeautyProfilePolicy {

    public static final int ALLERGY_ITEM_MAX_LENGTH = 50;

    public static final String ALLERGY_INCONSISTENCY = "알러지가 있다고 선택하셨습니다. 알러지 목록을 입력해주세요.";
    public static final String NO_ALLERGY_INCONSISTENCY = "알러지가 없다고 선택하셨습니다. 알러지 목록을 비워주세요.";
    public static final String ALLERGY_ITEM_SIZE = "알러지 항목은 " +
            BeautyProfilePolicy.ALLERGY_ITEM_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String PRICE_MIN_MAX_COMPARE = "최소 가격은 최대 가격보다 작거나 같아야 합니다.";
}
