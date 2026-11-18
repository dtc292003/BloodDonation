
package com.example.blooddonation.service;

import com.example.blooddonation.dto.*;
import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.exception.DuplicateResourceException;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpService emailOtpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // 1. Kiểm tra loại tài khoản
        if (request.roleName() != RoleName.USER) {
            throw new IllegalArgumentException(
                    "Hiện tại chỉ hỗ trợ đăng ký USER"
            );
        }

        // 2. Chuẩn hóa email
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 3. Kiểm tra email trùng
        if (userRepo.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email " + email + " đã được sử dụng"
            );
        }

        // 4. Kiểm tra giới hạn BCrypt
        if (request.password().getBytes(
                StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Mật khẩu không được vượt quá 72 byte"
            );
        }

        // 5. Lấy Role USER từ database
        Role role = roleRepo.findByRoleName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Hệ thống chưa được cấu hình Role USER"
                ));

        // 6. Tạo User
        User user = new User();

        user.setFullName(request.fullName().trim());
        user.setEmail(email);

        // Băm mật khẩu trước khi lưu
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setPhoneNumber(request.phoneNumber());
        user.setDateOfBirth(request.dateOfBirth());
        user.setGender(request.gender());
        user.setAddress(request.address());

        user.setRole(role);
        user.setEmailVerified(false);
        user.setUserStatus(UserStatus.PENDING_EMAIL);
        user.setCreatedAt(LocalDateTime.now());

        // 7. Lưu MySQL
        User savedUser = userRepo.save(user);
        emailOtpService.createAndSendOtp(savedUser.getUserId());

        return new RegisterResponse(
                savedUser.getUserId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().getRoleName(),
                savedUser.getUserStatus(),
                "Đăng ký thành công, cần xác minh email"
        );
    }

    @Transactional(readOnly = true)
    public User authenticateUser(LoginRequest request) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Email hoặc mật khẩu không đúng"
            );
        }
        if(!Boolean.TRUE.equals(user.getEmailVerified())){
            throw new DisabledException("Tài khoản chưa xác minh email");
        }
        if(user.getUserStatus() == UserStatus.LOCKED){
            throw new LockedException("Tài khoản đã bị khóa");
        }
        if(user.getUserStatus() !=UserStatus.ACTIVE){
            throw new DisabledException("Tài khoản chưa đươc phép đăng nhập");
        }
        return user;

    }

    //Login

    @Transactional
    public LoginResponse login(LoginRequest request){
        User user = authenticateUser(request);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenLifetimeSeconds(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getRoleName().name()
        );
    }


    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {

        // 1. Kiểm tra và xoay vòng Refresh Token
        RefreshTokenService.RotationResult result =
                refreshTokenService.rotate(request.refreshToken());

        User user = result.user();

        // 2. Tạo JWT Access Token mới
        String accessToken =
                jwtService.generateAccessToken(user);

        // 3. Trả về cặp Token mới
        return new LoginResponse(
                accessToken,
                result.refreshToken(),
                "Bearer",
                jwtService.getAccessTokenLifetimeSeconds(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getRoleName().name()
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {

        // Thu hồi Refresh Token hiện tại
        refreshTokenService.revoke(request.refreshToken());
    }

}
