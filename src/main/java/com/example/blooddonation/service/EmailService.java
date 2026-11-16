
package com.example.blooddonation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendTestEmail(String toEmail) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(toEmail);
        message.setSubject("BloodDonation - Kiểm tra gửi email");

        message.setText(
                "Xin chào!\n\n" +
                        "Đây là email kiểm thử từ hệ thống BloodDonation.\n" +
                        "Nếu bạn nhận được email này, " +
                        "chức năng gửi email SMTP đã hoạt động.\n\n" +
                        "Trân trọng,\n" +
                        "BloodDonation Team"
        );

        mailSender.send(message);
    }

    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(toEmail);

        message.setSubject("BloodDonation - Mã xác minh email");

        message.setText(
                "Xin chào!\n\n" +
                        "Mã OTP xác minh tài khoản BloodDonation của bạn là:\n\n" +
                        otp + "\n\n" +
                        "Mã có hiệu lực trong 5 phút.\n" +
                        "Không chia sẻ mã này với bất kỳ ai.\n\n" +
                        "Trân trọng,\n" +
                        "BloodDonation Team"
        );

        mailSender.send(message);
    }
}
