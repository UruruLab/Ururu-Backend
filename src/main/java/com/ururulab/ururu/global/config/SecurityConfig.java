package com.ururulab.ururu.global.config;

import com.ururulab.ururu.auth.filter.JwtAuthenticationFilter;
import com.ururulab.ururu.auth.handler.OAuth2LoginHandler;
import com.ururulab.ururu.auth.interceptor.RateLimitInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.function.Supplier;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private static final Set<String> CSRF_SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");
    private final RateLimitInterceptor rateLimitInterceptor;

    public SecurityConfig(final RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final JwtAuthenticationFilter jwtAuthenticationFilter,
            final OAuth2LoginHandler oAuth2LoginHandler,
            final ClientRegistrationRepository clientRegistrationRepository,
            final Environment environment,
            final ObjectProvider<CorsConfigurationSource> corsConfigurationSource,
            @Value("${app.cookie.domain:}") final String cookieDomain,
            @Value("${app.cookie.secure:false}") final boolean cookieSecure
    ) throws Exception {
        final CorsConfigurationSource cors = corsConfigurationSource.getIfAvailable();
        if (cors == null) {
            http.cors(AbstractHttpConfigurer::disable);
        } else {
            http.cors(configurer -> configurer.configurationSource(cors));
        }

        final CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        final boolean secureCookie = cookieSecure || StringUtils.hasText(cookieDomain);
        csrfRepository.setCookieCustomizer(cookie -> {
            cookie.path("/").secure(secureCookie).sameSite(secureCookie ? "None" : "Lax");
            if (StringUtils.hasText(cookieDomain)) {
                cookie.domain(cookieDomain.trim());
            }
        });

        final DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization"
                );
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                OAuth2AuthorizationRequestCustomizers.withPkce()
        );

        final boolean local = environment.acceptsProfiles(Profiles.of("local"));
        http.authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/auth/csrf",
                            "/api/auth/social/**",
                            "/api/auth/oauth/*",
                            "/api/auth/refresh",
                            "/api/auth/logout",
                            "/api/auth/me",
                            "/api/auth/seller/login",
                            "/api/auth/seller/logout",
                            "/api/auth/seller/refresh",
                            "/api/public/**"
                    ).permitAll();
                    auth.requestMatchers("/oauth2/authorization/**").permitAll();
                    auth.requestMatchers("/api/sellers/signup", "/api/sellers/check/**").permitAll();
                    auth.requestMatchers("/actuator/health").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/payments/webhooks/toss").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/ai/groupbuy-recommendations/health").permitAll();
                    auth.requestMatchers(
                            "/api/groupbuys/seller", "/api/groupbuys/seller/**",
                            "/api/groupbuys/statistics", "/api/groupbuys/statistics/**")
                            .hasRole("SELLER");
                    auth.requestMatchers(HttpMethod.GET, "/api/groupbuys/create").hasRole("SELLER");
                    auth.requestMatchers(HttpMethod.POST, "/api/groupbuys/**").hasRole("SELLER");
                    auth.requestMatchers(HttpMethod.PATCH, "/api/groupbuys/**").hasRole("SELLER");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/groupbuys/**").hasRole("SELLER");
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/groupbuys", "/api/groupbuys/*", "/api/groupbuys/top3", "/api/groupbuys/*/top6"
                    ).permitAll();
                    if (local) {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                    auth.requestMatchers(HttpMethod.DELETE, "/api/members/me").hasRole("MEMBER");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/members/*").hasRole("ADMIN");
                    auth.requestMatchers("/api/members/**", "/api/member/**", "/api/cart/**")
                            .hasRole("MEMBER");
                    auth.requestMatchers("/api/products/**").hasRole("SELLER");
                    auth.requestMatchers("/api/payments/**").hasRole("MEMBER");
                    auth.anyRequest().authenticated();
                })
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .requireCsrfProtectionMatcher(SecurityConfig::requiresCsrfProtection)
                        .ignoringRequestMatchers("/api/payments/webhooks/toss")
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth/*"))
                        .successHandler(oAuth2LoginHandler)
                        .failureHandler(oAuth2LoginHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static boolean requiresCsrfProtection(final HttpServletRequest request) {
        if (CSRF_SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }
        final String authorization = request.getHeader("Authorization");
        return authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7);
    }

    /** Spring Security 6 SPA 권장 방식: 응답은 BREACH 보호, 요청 헤더는 원문 토큰을 사용합니다. */
    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final Supplier<CsrfToken> csrfToken
        ) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(final HttpServletRequest request, final CsrfToken csrfToken) {
            return StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))
                    ? plain.resolveCsrfTokenValue(request, csrfToken)
                    : xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
