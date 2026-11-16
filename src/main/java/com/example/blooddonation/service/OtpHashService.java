
package com.example.blooddonation.service;

import com.example.blooddonation.enums.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Service
public class OtpHashService {

    private final SecretKeySpec secretKey;

    public OtpHashService(
            @Value("${security.otp.hmac-secret-base64}")
            String secret
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "Khóa HMAC phải có ít nhất 32 byte"
            );
        }

        this.secretKey =
                new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String hash(
            Integer userId,
            OtpPurpose purpose,
            String otp
    ) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            String data =
                    userId + ":" + purpose.name() + ":" + otp;

            byte[] result = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(result);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Không thể bảo vệ mã OTP", e
            );
        }
    }
}
