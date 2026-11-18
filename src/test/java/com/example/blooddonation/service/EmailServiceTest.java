
package com.example.blooddonation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    @EnabledIfEnvironmentVariable(
            named = "MAIL_TEST_TO",
            matches = ".+@.+"
    )
    void shouldSendRealEmail() {

        String recipient = System.getenv("MAIL_TEST_TO");

        emailService.sendTestEmail(recipient);
    }
    @Autowired
    private OtpGenerator otpGenerator;

    @Test
    @EnabledIfEnvironmentVariable(
            named = "MAIL_TEST_TO",
            matches = ".+@.+"
    )
    void shouldSendRealOtpEmail() {

        String recipient = System.getenv("MAIL_TEST_TO");
        String otp = otpGenerator.generateOtp();

        emailService.sendOtpEmail(recipient, otp);
    }
}
