package com.ururulab.ururu.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 설정 (개발 환경 전용).
 *
 * <p>백엔드 단독 테스트를 위한 CORS 설정입니다.
 * 운영 환경에서는 별도의 API Gateway나 프록시에서 CORS를 처리합니다.</p>
 */
@Slf4j
@Configuration
@Profile("dev")  // dev 환경에서만 활성화
public class CorsConfig {

    private static final long PREFLIGHT_CACHE_SECONDS = 3600L; // 1시간

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS for development environment");

        final CorsConfiguration configuration = createBaseCorsConfiguration();
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/auth/**", configuration);
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/health/**", configuration);

        source.registerCorsConfiguration("/static/**", createPermissiveCorsConfiguration());

        log.debug("CORS configuration registered for development environment");
        return source;
    }


    private CorsConfiguration createBaseCorsConfiguration() {
        final CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.vercel.app",
                "https://ururu-beauty.vercel.app"
        ));

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
        configuration.setMaxAge(PREFLIGHT_CACHE_SECONDS);

        return configuration;
    }

    private CorsConfiguration createPermissiveCorsConfiguration() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(PREFLIGHT_CACHE_SECONDS);
        return configuration;
    }
}