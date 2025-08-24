package com.ururulab.ururu.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 환경 관련 공통 유틸리티 클래스.
 * 여러 클래스에서 중복되는 환경 체크 로직을 중앙화합니다.
 */
@Slf4j
@Component
public final class EnvironmentHelper {

    private final Environment environment;

    public EnvironmentHelper(Environment environment) {
        this.environment = environment;
    }

    /**
     * 현재 환경이 개발환경인지 확인합니다.
     */
    public boolean isDevelopmentEnvironment() {
        try {
            return environment.acceptsProfiles("dev");
        } catch (Exception e) {
            log.warn("Profile check failed, defaulting to production for security: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 현재 환경이 운영환경인지 확인합니다.
     */
    public boolean isProductionEnvironment() {
        try {
            return environment.acceptsProfiles("prod");
        } catch (Exception e) {
            log.error("Profile check failed, defaulting to production for safety: {}", e.getMessage());
            return true; // 안전을 위해 프로덕션으로 간주
        }
    }

    /**
     * 현재 활성 프로파일을 반환합니다 (로깅용).
     */
    public String getCurrentProfile() {
        try {
            final String[] activeProfiles = environment.getActiveProfiles();
            return activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 환경별 프론트엔드 기본 URL을 결정합니다.
     */
    public String getFrontendBaseUrl() {
        try {
            final String frontendUrl = environment.getProperty("app.frontend.base-url");
            if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
                return frontendUrl.trim();
            }
            
            return getDefaultFrontendUrl();
            
        } catch (Exception e) {
            log.warn("Failed to get frontend URL from yml config, using fallback (env: {}): {}", 
                    getCurrentProfile(), e.getMessage());
            return getDefaultFrontendUrl();
        }
    }

    /**
     * 기본 프론트엔드 URL을 반환합니다.
     */
    private String getDefaultFrontendUrl() {
        return isProductionEnvironment() ? "https://www.ururu.shop" : "http://localhost:3000";
    }

    /**
     * Environment 객체에 접근합니다.
     */
    public Environment getEnvironment() {
        return environment;
    }
}
