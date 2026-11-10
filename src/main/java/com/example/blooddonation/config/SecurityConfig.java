
package com.example.blooddonation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // Tắt CSRF để kiểm tra API CRUD bằng Postman
                .csrf(csrf -> csrf.disable())

                // Cho phép truy cập API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                )

                // Tắt đăng nhập mặc định
                .formLogin(form -> form.disable())

                // Tắt HTTP Basic
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
