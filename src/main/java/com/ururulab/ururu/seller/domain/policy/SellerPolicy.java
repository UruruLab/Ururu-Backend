package com.ururulab.ururu.seller.domain.policy;

import com.ururulab.ururu.seller.domain.dto.validation.SellerValidationMessages;
import com.ururulab.ururu.seller.domain.dto.validation.SellerValidationPatterns;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SellerPolicy {

    // name (브랜드명) 유효성 검증
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.NAME_REQUIRED);
        }
    }

    // businessName (사업자명) 유효성 검증
    public static void validateBusinessName(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.BUSINESS_NAME_REQUIRED);
        }
    }

    // ownerName (대표 CEO명) 유효성 검증
    public static void validateOwnerName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.OWNER_NAME_REQUIRED);
        }
    }

    // businessNumber (사업자등록번호) 유효성 검증
    public static void validateBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.BUSINESS_NUMBER_REQUIRED);
        }
        if (!businessNumber.matches(SellerValidationPatterns.BUSINESS_NUMBER_PATTERN)) {
            throw new IllegalArgumentException(SellerValidationMessages.BUSINESS_NUMBER_PATTERN);
        }
    }

    // email 유효성 검증
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.EMAIL_REQUIRED);
        }
        if (!email.matches(SellerValidationPatterns.EMAIL_PATTERN)) {
            throw new IllegalArgumentException(SellerValidationMessages.EMAIL_FORMAT);
        }
    }

    // password 유효성 검증
    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.PASSWORD_REQUIRED);
        }
        if (!password.matches(SellerValidationPatterns.PASSWORD_PATTERN)) {
            throw new IllegalArgumentException(SellerValidationMessages.PASSWORD_PATTERN);
        }
    }

    // phone 유효성 검증
    public static void validatePhone(String phone) {
        if (phone == null || !phone.matches(SellerValidationPatterns.PHONE_PATTERN)) {
            throw new IllegalArgumentException(SellerValidationMessages.PHONE_FORMAT);
        }
    }

    // address1 유효성 검증
    public static void validateAddress1(String address1) {
        if (address1 == null || address1.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.ADDRESS1_REQUIRED);
        }
    }

    // mailOrderNumber (통신판매업 신고번호) 유효성 검증
    public static void validateMailOrderNumber(String mailOrderNumber) {
        if (mailOrderNumber == null || mailOrderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerValidationMessages.MAIL_ORDER_NUMBER_REQUIRED);
        }
    }
}