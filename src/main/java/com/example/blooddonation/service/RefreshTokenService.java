
package com.example.blooddonation.service;

import com.example.blooddonation.entity.RefreshToken;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepo;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(7);

    public record RotationResult(
            User user,
            String refreshToken
    ) {}

    //Cấp Refresh Token mới
    @Transactional
    public String issue(User user) {

        validateUser(user);

        return createAndSave(user);
    }

    // Sinh token và lưu bản băm vào MySQL
    private String createAndSave(User user) {

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        Instant now = Instant.now();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(
                now.plus(REFRESH_TOKEN_LIFETIME)
        );
        refreshToken.setRevokedAt(null);

        refreshTokenRepo.save(refreshToken);

        return rawToken;
    }

    // 3. Băm SHA-256
    private String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "Không hỗ trợ SHA-256", ex
            );
        }
    }

    // 4. Kiểm tra trạng thái tài khoản
    private void validateUser(User user) {

        if (user == null ||
                !Boolean.TRUE.equals(user.getEmailVerified()) ||
                user.getUserStatus() != UserStatus.ACTIVE) {

            throw new DisabledException(
                    "Tài khoản không được phép sử dụng Refresh Token"
            );
        }
    }

    // 5. Xoay vòng Refresh Token
    @Transactional
    public RotationResult rotate(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException(
                    "Refresh Token không hợp lệ"
            );
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken storedToken = refreshTokenRepo
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Refresh Token không hợp lệ"
                        )
                );

        Instant now = Instant.now();

        if (storedToken.getRevokedAt() != null ||
                !storedToken.getExpiresAt().isAfter(now)) {

            throw new BadCredentialsException(
                    "Refresh Token đã hết hạn hoặc bị thu hồi"
            );
        }

        User user = storedToken.getUser();

        validateUser(user);

        // Thu hồi token cũ
        storedToken.setRevokedAt(now);
        refreshTokenRepo.save(storedToken);

        // Cấp token mới
        String newRefreshToken = createAndSave(user);

        return new RotationResult(
                user,
                newRefreshToken
        );
    }

    // 6. Thu hồi token khi Logout
    @Transactional
    public void revoke(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepo.findByTokenHash(tokenHash)
                .ifPresent(storedToken -> {

                    if (storedToken.getRevokedAt() == null) {
                        storedToken.setRevokedAt(
                                Instant.now()
                        );

                        refreshTokenRepo.save(storedToken);
                    }
                });
    }
}
