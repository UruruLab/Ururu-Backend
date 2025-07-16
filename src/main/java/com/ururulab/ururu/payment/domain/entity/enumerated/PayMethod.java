package com.ururulab.ururu.payment.domain.entity.enumerated;

public enum PayMethod {
    CARD,       // 카드 결제
    TRANSFER,   // 계좌이체
    EASY_PAY;   // 간편결제 (토스페이, 카카오페이 등 모두 포함)

    public static PayMethod from(String method, String easyPayProvider) {
        if (method == null) {
            throw new IllegalArgumentException("결제 방식은 필수입니다.");
        }

        if ("카드".equals(method) && easyPayProvider == null) {
            return CARD;
        } else if ("계좌이체".equals(method) && easyPayProvider == null) {
            return TRANSFER;
        } else if ("간편결제".equals(method)) {
            return EASY_PAY;  // 간편결제는 모두 하나로 처리
        }

        throw new IllegalArgumentException("알 수 없는 결제 방식: " + method + ", " + easyPayProvider);
    }
}