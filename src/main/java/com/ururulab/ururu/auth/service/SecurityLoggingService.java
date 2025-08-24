package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.AuthConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 보안 로깅을 중앙화하는 서비스.
 * 민감한 정보 마스킹과 보안 이벤트 로깅을 담당합니다.
 */
@Slf4j
@Service
public class SecurityLoggingService {

    /**
     * 토큰을 마스킹합니다.
     *
     * @param token 마스킹할 토큰
     * @return 마스킹된 토큰
     */
    public String maskToken(String token) {
        if (token == null || token.length() <= AuthConstants.SENSITIVE_DATA_PREVIEW_LENGTH) {
            return AuthConstants.MASKED_DATA_PLACEHOLDER;
        }
        return token.substring(0, AuthConstants.SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
    }

    /**
     * 이메일을 마스킹합니다.
     *
     * @param email 마스킹할 이메일
     * @return 마스킹된 이메일
     */
    public String maskEmail(String email) {
        if (email == null || email.length() <= 3) {
            return AuthConstants.MASKED_DATA_PLACEHOLDER;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return AuthConstants.MASKED_DATA_PLACEHOLDER;
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domainPart;
        }
        
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
    }

    /**
     * 민감한 데이터를 마스킹합니다.
     *
     * @param data 마스킹할 데이터
     * @return 마스킹된 데이터
     */
    public String maskSensitiveData(String data) {
        if (data == null || data.length() <= AuthConstants.SENSITIVE_DATA_PREVIEW_LENGTH) {
            return AuthConstants.MASKED_DATA_PLACEHOLDER;
        }
        return data.substring(0, AuthConstants.SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
    }


} 