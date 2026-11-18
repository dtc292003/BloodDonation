
package com.example.blooddonation.service;

import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.RoleName;

import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void shouldGenerateAndVerifyAccessToken() {

        // 1. Khóa giả lập chỉ dùng cho Unit Test
        byte[] keyBytes =
                "0123456789abcdef0123456789abcdef"
                        .getBytes(StandardCharsets.UTF_8);

        SecretKey secretKey = new SecretKeySpec(
                keyBytes, "HmacSHA256"
        );

        // 2. Tạo JWT Encoder thật
        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        // 3. Tạo JWT Decoder thật
        JwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // 4. Tạo USER giả lập
        Role role = new Role();
        role.setRoleName(RoleName.USER);

        User user = new User();
        user.setUserId(14);
        user.setRole(role);

        // 5. Sinh token có hạn 15 phút
        JwtService jwtService = new JwtService(
                encoder, 15
        );

        String token = jwtService.generateAccessToken(user);

        // 6. Giải mã và kiểm tra chữ ký
        Jwt jwt = decoder.decode(token);

        // 7. Kiểm tra thông tin JWT
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals("14", jwt.getSubject());
        assertEquals("USER",
                jwt.getClaimAsString("role"));

        assertEquals("access",
                jwt.getClaimAsString("token_type"));

        assertNotNull(jwt.getExpiresAt());
        assertTrue(
                jwt.getExpiresAt().isAfter(Instant.now())
        );
    }
}
