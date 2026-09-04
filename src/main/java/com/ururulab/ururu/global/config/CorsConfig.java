package com.ururulab.ururu.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "app.cors.enabled", havingValue = "true")
public class CorsConfig {

    private static final long PREFLIGHT_CACHE_SECONDS = 3600L;

    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors.allowed-origins}") final String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-XSRF-TOKEN",
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

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
