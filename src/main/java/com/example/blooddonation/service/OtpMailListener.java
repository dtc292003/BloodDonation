
package com.example.blooddonation.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OtpMailListener {

    private static final Logger log =
            LoggerFactory.getLogger(OtpMailListener.class);

    private final EmailService emailService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void sendOtp(OtpMailEvent event) {

        try {
            emailService.sendOtpEmail(
                    event.toEmail(),
                    event.otp()
            );

        } catch (MailException ex) {
            log.error("Không thể gửi email OTP", ex);
        }
    }
}
