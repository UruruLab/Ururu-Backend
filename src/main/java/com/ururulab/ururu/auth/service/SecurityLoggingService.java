package com.ururulab.ururu.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 보안 로깅을 중앙화하는 서비스.
 * 민감한 정보 마스킹과 보안 이벤트 로깅을 담당합니다.
 */
@Slf4j
@Service
public class SecurityLoggingService {

    private static final int SENSITIVE_DATA_PREVIEW_LENGTH = 8;
    private static final String MASKED_DATA_PLACEHOLDER = "***";

    /**
     * 토큰을 마스킹합니다.
     *
     * @param token 마스킹할 토큰
     * @return 마스킹된 토큰
     */
    public String maskToken(String token) {
        if (token == null || token.length() <= SENSITIVE_DATA_PREVIEW_LENGTH) {
            return MASKED_DATA_PLACEHOLDER;
        }
        return token.substring(0, SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
    }

    /**
     * 이메일을 마스킹합니다.
     *
     * @param email 마스킹할 이메일
     * @return 마스킹된 이메일
     */
    public String maskEmail(String email) {
        if (email == null || email.length() <= 3) {
            return MASKED_DATA_PLACEHOLDER;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return MASKED_DATA_PLACEHOLDER;
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
        if (data == null || data.length() <= SENSITIVE_DATA_PREVIEW_LENGTH) {
            return MASKED_DATA_PLACEHOLDER;
        }
        return data.substring(0, SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
    }

    /**
     * 보안 이벤트를 로깅합니다.
     *
     * @param event 이벤트 타입
     * @param details 이벤트 상세 정보
     */
    public void logSecurityEvent(String event, String details) {
        log.info("Security Event - Type: {}, Details: {}", event, details);
    }

    /**
     * 로그인 시도를 로깅합니다.
     *
     * @param provider 제공자
     * @param email 이메일 (마스킹됨)
     * @param ip 클라이언트 IP
     * @param userAgent User-Agent
     * @param result 결과
     */
    public void logLoginAttempt(String provider, String email, String ip, String userAgent, String result) {
        log.info("Login Attempt - Provider: {}, Email: {}, IP: {}, UserAgent: {}, Result: {}", 
                provider, maskEmail(email), ip, userAgent, result);
    }

    /**
     * 토큰 관련 이벤트를 로깅합니다.
     *
     * @param event 이벤트 타입
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     * @param tokenId 토큰 ID (마스킹됨)
     */
    public void logTokenEvent(String event, Long userId, String userType, String tokenId) {
        log.info("Token Event - Type: {}, User: {} ({}), Token: {}", 
                event, userId, userType, maskSensitiveData(tokenId));
    }
} 