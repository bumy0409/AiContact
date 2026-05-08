package com.aicontact.backend.global.config;

import com.aicontact.backend.auth.jwt.JwtFilter;
import com.aicontact.backend.auth.jwt.JwtUtil;
import com.aicontact.backend.auth.jwt.LoginFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, JwtUtil jwtUtil) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CORS
        http.cors((cors) -> cors.configurationSource(new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(
                        "https://ai-contact-rho.vercel.app", "http://localhost:5173"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Collections.singletonList("*"));
                configuration.setAllowCredentials(true);
                configuration.setExposedHeaders(Collections.singletonList("Authorization"));
                configuration.setMaxAge(36000L);
                return configuration;
            }
        }));

        // CSRF / 기본 인증 비활성화 (JWT 기반)
        http.csrf((auth) -> auth.disable());
        http.formLogin((auth) -> auth.disable());
        http.httpBasic((auth) -> auth.disable());

        // 인가 정책
        http.authorizeHttpRequests((auth) -> auth
                // CORS Preflight 허용
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 회원가입 경로 허용
                .requestMatchers(HttpMethod.POST, "/users/sign-up").permitAll()

                // 로그인 경로 허용
                .requestMatchers("/auth/**").permitAll()

                // 에러 페이지 허용
                .requestMatchers("/error").permitAll()

                // baby chat 경로 허용
                .requestMatchers("/chat/**").permitAll()
                .requestMatchers("/summary/**").permitAll()
                .requestMatchers("/ws-chat/**", "/ws-baby/**", "/pub/**", "/sub/**").permitAll()

                // 그 외는 인증 필요
                .anyRequest().authenticated());

        // JWT 필터는 UsernamePasswordAuthenticationFilter 앞에 배치
        http.addFilterBefore(new JwtFilter(jwtUtil), LoginFilter.class);

        // 로그인 처리 필터는 UsernamePasswordAuthenticationFilter 위치에 배치
        http.addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil),
                UsernamePasswordAuthenticationFilter.class);

        // 세션 미사용
        http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}