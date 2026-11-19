
package com.example.blooddonation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // REST API dùng Bearer Token
                .csrf(AbstractHttpConfigurer::disable)

                // Không sử dụng HTTP Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Phân quyền API
                .authorizeHttpRequests(auth -> auth

                        // 1. Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 2. Authentication API công khai
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/register-medical",
                                "/api/auth/verify-email",
                                "/api/auth/resend-otp",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()

                        // 3. Quản lý Role: chỉ ADMIN
                        .requestMatchers("/api/roles/**")
                        .hasRole("ADMIN")

                        // 4. Quản lý User: tạm thời chỉ ADMIN
                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")

                        // 5. Quản lý đơn vị y tế: chỉ ADMIN
                        .requestMatchers("/api/medical-units/**")
                        .hasRole("ADMIN")

                        // 6. Các API khác yêu cầu đăng nhập
                        .anyRequest().authenticated()
                )

                // Xác thực JWT Bearer Token
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter rolesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Đọc claim "role" trong JWT
        rolesConverter.setAuthoritiesClaimName("role");

        // USER    -> ROLE_USER
        // MEDICAL -> ROLE_MEDICAL
        // ADMIN   -> ROLE_ADMIN
        rolesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                rolesConverter
        );

        return converter;
    }
}
