
package com.example.blooddonation.service;

import com.example.blooddonation.dto.RegisterRequest;
import com.example.blooddonation.dto.RegisterResponse;
import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.exception.DuplicateResourceException;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;

import lombok.RequiredArgsConstructor;
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
}
