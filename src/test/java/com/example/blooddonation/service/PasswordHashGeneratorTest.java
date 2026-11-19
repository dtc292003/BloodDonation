
package com.example.blooddonation;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashGeneratorTest {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String password = "Medical@12345";

        System.out.println(
                "BCrypt Hash: " + encoder.encode(password)
        );
    }
}
