package com.ururulab.ururu.global.common.util;

import lombok.experimental.UtilityClass;

/**
 * 민감한 정보 마스킹을 위한 유틸리티 클래스
 * 로그 출력 시 개인정보 보호를 위해 사용
 */
@UtilityClass
public class MaskingUtils {

    /**
     * 이메일 마스킹
     */
    public static String maskEmail(final String email) {
        if (email == null || email.length() <= 3) {
            return "***";
        }
        final int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email.substring(0, 1) + "***@" + email.substring(atIndex + 1);
        }
        return email.substring(0, 1) + "***@" + email.substring(atIndex + 1);
    }

    /**
     * 사업자등록번호 마스킹
     */
    public static String maskBusinessNumber(final String businessNumber) {
        if (businessNumber == null || businessNumber.length() <= 4) {
            return "***";
        }
        return businessNumber.substring(0, 3) + "****" + businessNumber.substring(7);
    }

    /**
     * 전화번호 마스킹
     */
    public static String maskPhone(final String phone) {
        if (phone == null || phone.length() <= 4) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 이름 마스킹
     */
    public static String maskName(final String name) {
        if (name == null || name.length() <= 1) {
            return "***";
        }
        if (name.length() == 2) {
            return name.substring(0, 1) + "*";
        }
        return name.substring(0, 1) + "*" + name.substring(name.length() - 1);
    }
} 