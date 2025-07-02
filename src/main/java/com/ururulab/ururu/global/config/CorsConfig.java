package com.ururulab.ururu.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 설정.
 *
 * <p>환경별로 다른 CORS 정책을 적용합니다.
 * 허용 도메인은 app.cors.allowed-origins 프로퍼티에서 설정합니다.</p>
 */
@Slf4j
@Configuration
public class CorsConfig {

    private static final long PREFLIGHT_CACHE_SECONDS = 3600L;

    private final Environment environment;

    @Value("${app.cors.allowed-origins:#{null}}")
    private List<String> allowedOrigins;

    public CorsConfig(final Environment environment) {
        this.environment = environment;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final boolean isDev = isDevelopmentProfile();
        
        // allowedOrigins가 null이거나 비어있는 경우 기본값 설정
        List<String> origins = allowedOrigins;
        if (origins == null || origins.isEmpty()) {
            log.warn("CORS allowed-origins not configured, using default values");
            origins = isDev ? 
                List.of("http://localhost:*", "http://127.0.0.1:*") : 
                List.of("https://ururu.shop");
        }
        
        log.info("Configuring CORS for {} environment with origins: {}", 
                isDev ? "development" : "production", origins);

        final CorsConfiguration configuration = new CorsConfiguration();

        // 환경에 따른 Origin 설정
        if (isDev) {
            // 개발환경: 패턴 매칭 허용 (와일드카드 지원)
            configuration.setAllowedOriginPatterns(origins);
        } else {
            // 운영환경: 정확한 도메인만 허용 (보안 강화)
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Cache-Control"
        ));

        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Length",
                "X-Total-Count"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(isDev ? PREFLIGHT_CACHE_SECONDS : 3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/auth/**", configuration);
        source.registerCorsConfiguration("/health/**", configuration);

        // 개발환경에서만 static 리소스에 대한 관대한 CORS 정책 적용
        if (isDev) {
            source.registerCorsConfiguration("/static/**", createPermissiveCorsConfiguration());
        }

        log.debug("CORS configuration registered for {} environment", isDev ? "development" : "production");
        return source;
    }

    /**
     * 안전한 프로파일 확인 (null-safe)
     */
    private boolean isDevelopmentProfile() {
        try {
            return environment.acceptsProfiles("dev");
        } catch (Exception e) {
            log.debug("Profile check failed, defaulting to development: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 개발환경 전용: static 리소스에 대한 관대한 CORS 정책
     */
    private CorsConfiguration createPermissiveCorsConfiguration() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(PREFLIGHT_CACHE_SECONDS);
        return configuration;
    }
}