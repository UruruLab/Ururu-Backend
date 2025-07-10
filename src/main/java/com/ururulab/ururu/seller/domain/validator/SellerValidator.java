package com.ururulab.ururu.seller.domain.validator;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
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
            throw new BusinessException(ErrorCode.SELLER_NAME_REQUIRED);
        }
        if (name.trim().length() > SellerConstants.NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_NAME_TOO_LONG, SellerConstants.NAME_MAX_LENGTH);
        }
    }

    // businessName (사업자명) 유효성 검증
    public static void validateBusinessName(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NAME_REQUIRED);
        }
        if (businessName.trim().length() > SellerConstants.BUSINESS_NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NAME_TOO_LONG, SellerConstants.BUSINESS_NAME_MAX_LENGTH);
        }
    }

    // ownerName (대표 CEO명) 유효성 검증
    public static void validateOwnerName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_OWNER_NAME_REQUIRED);
        }
        if (ownerName.trim().length() > SellerConstants.OWNER_NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_OWNER_NAME_TOO_LONG, SellerConstants.OWNER_NAME_MAX_LENGTH);
        }
    }

    // businessNumber (사업자등록번호) 유효성 검증
    public static void validateBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_REQUIRED);
        }
        String trimmedNumber = businessNumber.trim();
        if (trimmedNumber.length() != SellerConstants.BUSINESS_NUMBER_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_TOO_LONG, SellerConstants.BUSINESS_NUMBER_LENGTH);
        }
        if (!trimmedNumber.matches(SellerConstants.BUSINESS_NUMBER_PATTERN)) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_PATTERN_ERROR);
        }
    }

    // email 유효성 검증
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_EMAIL_REQUIRED);
        }

        // 이메일 정규화 (소문자 변환)
        final String normalizedEmail = email.trim().toLowerCase();
        
        if (normalizedEmail.length() > SellerConstants.EMAIL_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_EMAIL_TOO_LONG, SellerConstants.EMAIL_MAX_LENGTH);
        }
        
        if (!normalizedEmail.matches(SellerConstants.EMAIL_PATTERN)) {
            throw new BusinessException(ErrorCode.SELLER_EMAIL_FORMAT_ERROR);
        }
    }

    // email 정규화 및 검증 (정규화된 이메일 반환)
    public static String normalizeAndValidateEmail(String email) {
        validateEmail(email);
        return email.trim().toLowerCase();
    }

    // password 유효성 검증
    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_PASSWORD_REQUIRED);
        }
        if (password.length() > SellerConstants.PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_PASSWORD_SIZE_ERROR);
        }
        if (!password.matches(SellerConstants.PASSWORD_PATTERN)) {
            throw new BusinessException(ErrorCode.SELLER_PASSWORD_PATTERN_ERROR);
        }
    }

    // phone 유효성 검증
    public static void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_PHONE_REQUIRED);
        }
        String trimmedPhone = phone.trim();
        if (trimmedPhone.length() > SellerConstants.PHONE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_PHONE_TOO_LONG, SellerConstants.PHONE_MAX_LENGTH);
        }
        if (!trimmedPhone.matches(SellerConstants.PHONE_PATTERN)) {
            throw new BusinessException(ErrorCode.SELLER_PHONE_FORMAT_ERROR);
        }
    }

    // address1 유효성 검증
    public static void validateAddress1(String address1) {
        if (address1 == null || address1.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_ADDRESS1_REQUIRED);
        }
        if (address1.trim().length() > SellerConstants.ADDRESS_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_ADDRESS1_TOO_LONG, SellerConstants.ADDRESS_MAX_LENGTH);
        }
    }

    // address2 유효성 검증
    public static void validateAddress2(String address2) {
        if (address2 == null || address2.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_ADDRESS2_REQUIRED);
        }
        if (address2.trim().length() > SellerConstants.ADDRESS_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_ADDRESS2_TOO_LONG, SellerConstants.ADDRESS_MAX_LENGTH);
        }
    }

    // mailOrderNumber (통신판매업 신고번호) 유효성 검증
    public static void validateMailOrderNumber(String mailOrderNumber) {
        if (mailOrderNumber == null || mailOrderNumber.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SELLER_MAIL_ORDER_NUMBER_REQUIRED);
        }
        if (mailOrderNumber.trim().length() > SellerConstants.MAIL_ORDER_NUMBER_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SELLER_MAIL_ORDER_NUMBER_TOO_LONG, SellerConstants.MAIL_ORDER_NUMBER_MAX_LENGTH);
        }
    }
} 