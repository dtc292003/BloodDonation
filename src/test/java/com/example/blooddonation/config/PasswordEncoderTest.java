
package com.example.blooddonation.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder =
            new PasswordConfig().passwordEncoder();

    @Test
    void shouldEncodeAndVerifyPassword() {

        // Mật khẩu gốc
        String rawPassword = "Test@123456";

        // Băm mật khẩu
        String hashedPassword =
                passwordEncoder.encode(rawPassword);

        // Kiểm tra mật khẩu băm khác mật khẩu gốc
        assertNotEquals(rawPassword, hashedPassword);

        // Kiểm tra mật khẩu đúng
        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        hashedPassword
                )
        );

        // Kiểm tra mật khẩu sai
        assertFalse(
                passwordEncoder.matches(
                        "WrongPassword",
                        hashedPassword
                )
        );
    }
}
