package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 보안 설정
 * - JWT 기반 인증
 * - 엔드포인트별 권한 제어
 * - CORS 설정
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Value("${cors.allowed-origins:http://localhost:8080,http://localhost:3000}")
    private String allowedOrigins;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // 공개 엔드포인트 (인증 불필요)
                auth.requestMatchers(
                    "/api/auth/**",  // 모든 인증 관련 엔드포인트 허용
                    "/actuator/health",
                    "/actuator/prometheus",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/ws",          // WebSocket 엔드포인트
                    "/ws/**",       // WebSocket SockJS fallback
                    "/chat",        // WebSocket 엔드포인트
                    "/chat/**",     // WebSocket SockJS fallback
                    "/",
                    "/index.html",
                    "/chat.html",
                    "/manifest.json",
                    "/sw.js",
                    "/css/**",      // CSS 파일
                    "/js/**",       // JS 파일
                    "/assets/**",   // Assets 파일
                    "/*.css",
                    "/*.js",
                    "/static/**"
                ).permitAll();

                // H2 콘솔은 개발 환경(dev, local)에서만 허용
                if (isDevEnvironment()) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }

                // 나머지 모든 요청은 인증 필요
                auth.anyRequest().authenticated();
            });

        // H2 콘솔 iframe 허용 (개발 환경에서만)
        if (isDevEnvironment()) {
            http.headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable()));
        }

        http
            // Rate Limiting 필터 추가 (가장 먼저)
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 환경변수에서 허용된 오리진 목록 파싱 (쉼표로 구분)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins.stream().map(String::trim).toList());

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // preflight 캐싱 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 개발 환경 여부 확인
     */
    private boolean isDevEnvironment() {
        return "dev".equalsIgnoreCase(activeProfile) ||
               "local".equalsIgnoreCase(activeProfile) ||
               "development".equalsIgnoreCase(activeProfile);
    }
}