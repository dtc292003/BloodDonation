
package com.example.blooddonation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // 1. Cho phép React Web gọi Backend
                .cors(Customizer.withDefaults())

                // 2. REST API sử dụng JWT Bearer Token
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Không sử dụng HTTP Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // 4. Phân quyền API
                .authorizeHttpRequests(auth -> auth

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Authentication API công khai
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/register-medical-with-document",
                                "/api/auth/verify-email",
                                "/api/auth/resend-otp",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout"
                        ).permitAll()

                        // Role: ADMIN
                        .requestMatchers("/api/roles/**")
                        .hasRole("ADMIN")

                        // User: ADMIN
                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")

                        // Medical Unit: ADMIN
                        .requestMatchers("/api/medical-units/**")
                        .hasRole("ADMIN")

                        // Các API khác phải đăng nhập
                        .anyRequest().authenticated()
                )

                // 5. Xác thực JWT Bearer Token
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }

    // 6. Ánh xạ Role từ JWT
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter rolesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Đọc claim "role"
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

    // 7. Cấu hình CORS cho React Frontend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // React + Vite
        config.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        // Các phương thức HTTP được phép
        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Các header được phép gửi
        config.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        // Hiện tại sử dụng JWT Bearer, không dùng cookie
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // Áp dụng cho toàn bộ API
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
