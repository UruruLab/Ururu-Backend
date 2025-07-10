package com.ururulab.ururu.seller.domain.validator;

import com.ururulab.ururu.seller.domain.constant.SellerConstants;
import lombok.experimental.UtilityClass;

/**
 * 판매자 도메인 검증 로직을 담당하는 클래스.
 * 판매자 관련 데이터의 유효성을 검증합니다.
 */
@UtilityClass
public class SellerValidator {

    // name (브랜드명) 유효성 검증
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.NAME_REQUIRED);
        }
    }

    // businessName (사업자명) 유효성 검증
    public static void validateBusinessName(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.BUSINESS_NAME_REQUIRED);
        }
    }

    // ownerName (대표 CEO명) 유효성 검증
    public static void validateOwnerName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.OWNER_NAME_REQUIRED);
        }
    }

    // businessNumber (사업자등록번호) 유효성 검증
    public static void validateBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.BUSINESS_NUMBER_REQUIRED);
        }
        if (!businessNumber.trim().matches(SellerConstants.BUSINESS_NUMBER_PATTERN)) {
            throw new IllegalArgumentException(SellerConstants.BUSINESS_NUMBER_PATTERN_ERROR);
        }
    }

    // email 유효성 검증
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.EMAIL_REQUIRED);
        }

        // 이메일 정규화 (소문자 변환)
        final String normalizedEmail = email.trim().toLowerCase();
        
        if (!normalizedEmail.matches(SellerConstants.EMAIL_PATTERN)) {
            throw new IllegalArgumentException(SellerConstants.EMAIL_FORMAT);
        }
    }

    // password 유효성 검증
    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.PASSWORD_REQUIRED);
        }
        if (password.length() > SellerConstants.PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException(SellerConstants.PASSWORD_SIZE);
        }
        if (!password.matches(SellerConstants.PASSWORD_PATTERN)) {
            throw new IllegalArgumentException(SellerConstants.PASSWORD_PATTERN_ERROR);
        }
    }

    // phone 유효성 검증
    public static void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.PHONE_REQUIRED);
        }
        if (!phone.trim().matches(SellerConstants.PHONE_PATTERN)) {
            throw new IllegalArgumentException(SellerConstants.PHONE_FORMAT);
        }
    }

    // address1 유효성 검증
    public static void validateAddress1(String address1) {
        if (address1 == null || address1.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.ADDRESS1_REQUIRED);
        }
    }

    // address2 유효성 검증
    public static void validateAddress2(String address2) {
        if (address2 == null || address2.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.ADDRESS2_REQUIRED);
        }
    }

    // mailOrderNumber (통신판매업 신고번호) 유효성 검증
    public static void validateMailOrderNumber(String mailOrderNumber) {
        if (mailOrderNumber == null || mailOrderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(SellerConstants.MAIL_ORDER_NUMBER_REQUIRED);
        }
    }
} 