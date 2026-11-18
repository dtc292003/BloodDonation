
package com.example.blooddonation.service;

import com.example.blooddonation.dto.LoginRequest;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;
import com.example.blooddonation.dto.LoginResponse;
import com.example.blooddonation.entity.Role;
import com.example.blooddonation.enums.RoleName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailOtpService emailOtpService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    // Tạo USER giả lập để tái sử dụng
    private User createTestUser(
            UserStatus status,
            boolean emailVerified
    ) {
        User user = new User();

        user.setUserId(1);
        user.setEmail("test@gmail.com");
        user.setPasswordHash("hashed-password");
        user.setEmailVerified(emailVerified);
        user.setUserStatus(status);

        return user;
    }

    // TEST 1: Đăng nhập thành công
    @Test
    void shouldAuthenticateUserWithValidCredentials() {

        User user = createTestUser(
                UserStatus.ACTIVE, true
        );

        LoginRequest request = new LoginRequest(
                "test@gmail.com", "Test@123456"
        );

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Test@123456", "hashed-password"
        )).thenReturn(true);

        User result = authService.authenticateUser(request);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals(UserStatus.ACTIVE, result.getUserStatus());

        verify(passwordEncoder, times(1))
                .matches("Test@123456", "hashed-password");
    }

    // TEST 2: Sai mật khẩu
    @Test
    void shouldRejectLoginWithWrongPassword() {

        User user = createTestUser(
                UserStatus.ACTIVE, true
        );

        LoginRequest request = new LoginRequest(
                "test@gmail.com", "WrongPassword"
        );

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword", "hashed-password"
        )).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticateUser(request)
        );

        assertEquals(
                "Email hoặc mật khẩu không đúng",
                exception.getMessage()
        );
    }

    // TEST 3: Email không tồn tại
    @Test
    void shouldRejectLoginWithUnknownEmail() {

        LoginRequest request = new LoginRequest(
                "unknown@gmail.com", "Test@123456"
        );

        when(userRepo.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticateUser(request)
        );

        assertEquals(
                "Email hoặc mật khẩu không đúng",
                exception.getMessage()
        );

        verifyNoInteractions(passwordEncoder);
    }

    // TEST 4: Email chưa xác minh
    @Test
    void shouldRejectLoginWhenEmailNotVerified() {

        User user = createTestUser(
                UserStatus.PENDING_EMAIL, false
        );

        LoginRequest request = new LoginRequest(
                "test@gmail.com", "Test@123456"
        );

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Test@123456", "hashed-password"
        )).thenReturn(true);

        DisabledException exception = assertThrows(
                DisabledException.class,
                () -> authService.authenticateUser(request)
        );

        assertEquals(
                "Tài khoản chưa xác minh email",
                exception.getMessage()
        );
    }

    // TEST 5: Tài khoản bị khóa
    @Test
    void shouldRejectLoginWhenUserLocked() {

        User user = createTestUser(
                UserStatus.LOCKED, true
        );

        LoginRequest request = new LoginRequest(
                "test@gmail.com", "Test@123456"
        );

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Test@123456", "hashed-password"
        )).thenReturn(true);

        LockedException exception = assertThrows(
                LockedException.class,
                () -> authService.authenticateUser(request)
        );

        assertEquals(
                "Tài khoản đã bị khóa",
                exception.getMessage()
        );
    }

    // TEST 6: MEDICAL chưa được duyệt
    @Test
    void shouldRejectLoginWhenMedicalPendingApproval() {

        User user = createTestUser(
                UserStatus.PENDING_APPROVAL, true
        );

        LoginRequest request = new LoginRequest(
                "test@gmail.com", "Test@123456"
        );

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Test@123456", "hashed-password"
        )).thenReturn(true);

        DisabledException exception = assertThrows(
                DisabledException.class,
                () -> authService.authenticateUser(request)
        );

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isBlank());
    }



    @Test
    void shouldLoginAndReturnAccessToken() {

        // 1. Tạo Role USER
        Role role = new Role();
        role.setRoleName(RoleName.USER);

        // 2. Tạo tài khoản hợp lệ
        User user = createTestUser(UserStatus.ACTIVE, true);
        user.setFullName("Nguyễn Văn Test");
        user.setRole(role);

        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "Test@123456"
        );

        // 3. Giả lập tìm người dùng
        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        // 4. Giả lập xác thực mật khẩu thành công
        when(passwordEncoder.matches(
                "Test@123456",
                "hashed-password"
        )).thenReturn(true);

        // 5. Giả lập tạo Access Token
        when(jwtService.generateAccessToken(user))
                .thenReturn("test-access-token");

        when(jwtService.getAccessTokenLifetimeSeconds())
                .thenReturn(900L);

        // 6. Giả lập tạo Refresh Token
        when(refreshTokenService.issue(user))
                .thenReturn("test-refresh-token");

        // 7. Gọi AuthService.login()
        LoginResponse response = authService.login(request);

        // 8. Kiểm tra kết quả trả về
        assertNotNull(response);

        assertEquals(
                "test-access-token",
                response.accessToken()
        );

        assertEquals(
                "test-refresh-token",
                response.refreshToken()
        );

        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresInSeconds());
        assertEquals(1, response.userId());
        assertEquals("Nguyễn Văn Test", response.fullName());
        assertEquals("test@gmail.com", response.email());
        assertEquals("USER", response.role());

        // 9. Kiểm tra các Service đã được gọi
        verify(jwtService, times(1))
                .generateAccessToken(user);

        verify(refreshTokenService, times(1))
                .issue(user);
    }


    @Test
    void shouldNotGenerateTokenWhenPasswordIsWrong() {

        User user = createTestUser(UserStatus.ACTIVE, true);

        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "WrongPassword"
        );

        when(userRepo.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                "hashed-password"
        )).thenReturn(false);

        // Đăng nhập phải thất bại
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        // Không được cấp JWT khi sai mật khẩu
        verifyNoInteractions(jwtService);
    }

}
