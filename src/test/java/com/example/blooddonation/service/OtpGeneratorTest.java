
package com.example.blooddonation.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtpGeneratorTest {

    private final OtpGenerator otpGenerator =
            new OtpGenerator();

    @Test
    void shouldGenerateSixDigitOtp() {

        for (int i = 0; i < 100; i++) {

            String otp = otpGenerator.generateOtp();

            assertNotNull(otp);
            assertTrue(otp.matches("\\d{6}"));
        }
    }
}
