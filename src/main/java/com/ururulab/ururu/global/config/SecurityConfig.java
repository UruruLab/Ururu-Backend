package com.ururulab.ururu.global.config;

import com.ururulab.ururu.auth.filter.JwtAuthenticationFilter;
import com.ururulab.ururu.auth.filter.CsrfTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfTokenFilter csrfTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 개발 환경용 Security 설정
     * - CSRF 보호 활성화 (JWT 쿠키 사용으로 인해 필요)
     * - H2 콘솔 접근 허용
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(
            final HttpSecurity http,
            @Qualifier("corsConfigurationSource") final CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/sellers/signup").permitAll()
                        .requestMatchers("/api/sellers/check/**").permitAll()
                        .requestMatchers("/api/ai/**").permitAll()  // AI API 전체 인증 제외
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys/top3").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys/*/top6").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/public/**",    // 공개 API
                                "/health",          // 헬스체크
                                "/h2-console/**"    // H2 콘솔
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                        .contentTypeOptions(contentType -> contentType.disable())
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig.disable())
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(csrfTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CsrfTokenFilter.class)
                .build();
    }
    
    /**
     * 운영 환경용 Security 설정
     * - CSRF 보호 활성화 (JWT 쿠키 사용으로 인해 필요)
     * - 강화된 보안 설정
     */
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodFilterChain(
            final HttpSecurity http,
            @Qualifier("corsConfigurationSource") final CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))  // 운영용 CORS 적용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/sellers/signup").permitAll()
                        .requestMatchers("/api/sellers/check/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys/top3").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/groupbuys/*/top6").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/public/**",    // 공개 API
                                "/health",          // 헬스체크
                                "/actuator/**"      // 모니터링 API
                        )
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(csrfTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CsrfTokenFilter.class)
                .build();
    }
}