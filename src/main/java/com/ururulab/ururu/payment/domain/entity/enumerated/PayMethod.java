package com.ururulab.ururu.payment.domain.entity.enumerated;

public enum PayMethod {
    CARD,       // 카드 결제
    TRANSFER,   // 계좌이체
    TOSS_PAY;   // 토스페이

    public static PayMethod from(String method, String easyPayProvider) {
        if (method == null) {
            throw new IllegalArgumentException("결제 방식은 필수입니다.");
        }
        if ("카드".equals(method) && easyPayProvider == null) {
            return CARD;
        } else if ("계좌이체".equals(method) && easyPayProvider == null) {
            return TRANSFER;
        } else if ("카드".equals(method) && "토스페이".equals(easyPayProvider)) {
            return TOSS_PAY;
        }
        throw new IllegalArgumentException("알 수 없는 결제 방식: " + method + ", " + easyPayProvider);
    }
}