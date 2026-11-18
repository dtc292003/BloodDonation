
package com.example.blooddonation.service;

import com.example.blooddonation.entity.RefreshToken;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.repository.RefreshTokenRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepo;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    // Tạo USER hợp lệ
    private User createActiveUser() {
        User user = new User();
        user.setUserId(14);
        user.setEmailVerified(true);
        user.setUserStatus(UserStatus.ACTIVE);
        return user;
    }

    // Tạo Refresh Token giả lập
    private RefreshToken createStoredToken(User user) {
        RefreshToken token = new RefreshToken();

        token.setRefreshTokenId(1);
        token.setUser(user);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(
                Instant.now().plusSeconds(3600)
        );
        token.setRevokedAt(null);

        return token;
    }

    // Tính SHA-256 để kiểm tra dữ liệu được lưu
    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    // TEST 1: Cấp Refresh Token mới
    @Test
    void shouldIssueRefreshToken() {

        User user = createActiveUser();

        String rawToken = refreshTokenService.issue(user);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepo).save(captor.capture());

        RefreshToken stored = captor.getValue();

        assertEquals(user, stored.getUser());
        assertEquals(64, stored.getTokenHash().length());

        // Database chỉ lưu hash, không lưu token gốc
        assertNotEquals(rawToken, stored.getTokenHash());
        assertEquals(
                sha256(rawToken),
                stored.getTokenHash()
        );

        assertNotNull(stored.getExpiresAt());
        assertNull(stored.getRevokedAt());
    }

    // TEST 2: Xoay vòng Refresh Token
    @Test
    void shouldRotateRefreshToken() {

        User user = createActiveUser();

        String oldRawToken = "old-refresh-token";

        RefreshToken oldToken = createStoredToken(user);
        oldToken.setTokenHash(sha256(oldRawToken));

        when(refreshTokenRepo.findByTokenHash(
                sha256(oldRawToken)
        )).thenReturn(Optional.of(oldToken));

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotate(oldRawToken);

        assertNotNull(result);
        assertEquals(user, result.user());
        assertNotNull(result.refreshToken());
        assertNotEquals(
                oldRawToken,
                result.refreshToken()
        );

        // Token cũ đã bị thu hồi
        assertNotNull(oldToken.getRevokedAt());

        // Lưu token cũ và token mới
        verify(refreshTokenRepo, times(2))
                .save(any(RefreshToken.class));
    }

    // TEST 3: Không cho dùng lại token đã thu hồi
    @Test
    void shouldRejectRevokedRefreshToken() {

        User user = createActiveUser();

        String rawToken = "revoked-token";

        RefreshToken token = createStoredToken(user);
        token.setTokenHash(sha256(rawToken));
        token.setRevokedAt(Instant.now());

        when(refreshTokenRepo.findByTokenHash(
                sha256(rawToken)
        )).thenReturn(Optional.of(token));

        assertThrows(
                BadCredentialsException.class,
                () -> refreshTokenService.rotate(rawToken)
        );

        verify(refreshTokenRepo, never())
                .save(any(RefreshToken.class));
    }

    // TEST 4: Token hết hạn
    @Test
    void shouldRejectExpiredRefreshToken() {

        User user = createActiveUser();

        String rawToken = "expired-token";

        RefreshToken token = createStoredToken(user);
        token.setTokenHash(sha256(rawToken));

        token.setExpiresAt(
                Instant.now().minusSeconds(60)
        );

        when(refreshTokenRepo.findByTokenHash(
                sha256(rawToken)
        )).thenReturn(Optional.of(token));

        assertThrows(
                BadCredentialsException.class,
                () -> refreshTokenService.rotate(rawToken)
        );

        verify(refreshTokenRepo, never())
                .save(any(RefreshToken.class));
    }

    // TEST 5: USER bị khóa
    @Test
    void shouldRejectRefreshWhenUserLocked() {

        User user = createActiveUser();
        user.setUserStatus(UserStatus.LOCKED);

        String rawToken = "locked-user-token";

        RefreshToken token = createStoredToken(user);
        token.setTokenHash(sha256(rawToken));

        when(refreshTokenRepo.findByTokenHash(
                sha256(rawToken)
        )).thenReturn(Optional.of(token));

        assertThrows(
                DisabledException.class,
                () -> refreshTokenService.rotate(rawToken)
        );

        verify(refreshTokenRepo, never())
                .save(any(RefreshToken.class));
    }

    // TEST 6: Logout thu hồi Refresh Token
    @Test
    void shouldRevokeRefreshToken() {

        User user = createActiveUser();

        String rawToken = "logout-token";

        RefreshToken token = createStoredToken(user);
        token.setTokenHash(sha256(rawToken));

        when(refreshTokenRepo.findByTokenHash(
                sha256(rawToken)
        )).thenReturn(Optional.of(token));

        refreshTokenService.revoke(rawToken);

        assertNotNull(token.getRevokedAt());

        verify(refreshTokenRepo, times(1))
                .save(token);
    }

    // TEST 7: Không chấp nhận token trống
    @Test
    void shouldRejectBlankRefreshToken() {

        assertThrows(
                BadCredentialsException.class,
                () -> refreshTokenService.rotate(" ")
        );

        verifyNoInteractions(refreshTokenRepo);
    }
}
