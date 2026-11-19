
package com.example.blooddonation.service;

import com.example.blooddonation.dto.*;
import com.example.blooddonation.entity.*;
import com.example.blooddonation.enums.*;
import com.example.blooddonation.exception.DuplicateResourceException;
import com.example.blooddonation.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MedicalRegistrationService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final MedicalUnitRepository medicalUnitRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpService emailOtpService;

    @Transactional
    public MedicalRegisterResponse register(
            MedicalRegisterRequest request
    ) {
        // 1. Kiểm tra đúng một phương án đăng ký đơn vị
        boolean hasUnitId = request.medicalUnitId() != null;
        boolean hasNewUnit = request.newMedicalUnit() != null;

        if (hasUnitId == hasNewUnit) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chọn đơn vị có sẵn hoặc khai báo đơn vị mới"
            );
        }

        // 2. Chuẩn hóa email
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepo.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email đã được sử dụng"
            );
        }

        // BCrypt chỉ xử lý tối đa 72 byte mật khẩu
        int passwordBytes = request.password()
                .getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu vượt quá giới hạn BCrypt"
            );
        }

        // 3. Lấy Role MEDICAL
        Role role = roleRepo.findByRoleName(RoleName.MEDICAL)
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa cấu hình Role MEDICAL"
                ));

        // 4. Xác định đơn vị y tế
        MedicalUnit unit;

        if (hasUnitId) {
            unit = medicalUnitRepo
                    .findById(request.medicalUnitId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy đơn vị y tế"
                    ));

            if (unit.getApprovalStatus()
                    != ApprovalStatus.APPROVED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn vị y tế chưa được phê duyệt"
                );
            }

        } else {
            unit = createMedicalUnit(
                    request.newMedicalUnit()
            );
        }

        // 5. Tạo tài khoản MEDICAL
        User user = new User();

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setPhoneNumber(request.phoneNumber());
        user.setDateOfBirth(request.dateOfBirth());
        user.setGender(request.gender());
        user.setAddress(request.address());

        user.setRole(role);
        user.setMedicalUnit(unit);

        user.setEmailVerified(false);
        user.setUserStatus(UserStatus.PENDING_EMAIL);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepo.save(user);

        // 6. Tạo OTP bằng hệ thống hiện có
        emailOtpService.createAndSendOtp(
                savedUser.getUserId()
        );

        return new MedicalRegisterResponse(
                savedUser.getUserId(),
                unit.getMedicalUnitId(),
                savedUser.getUserStatus(),
                "Đăng ký thành công. Vui lòng xác minh email."
        );
    }

    private MedicalUnit createMedicalUnit(
            NewMedicalUnitRequest request
    ) {
        MedicalUnit unit = new MedicalUnit();

        unit.setUnitName(request.unitName().trim());
        unit.setUnitType(request.unitType().trim());
        unit.setAddress(request.address().trim());

        unit.setProvince(request.province());
        unit.setDistrict(request.district());
        unit.setWard(request.ward());

        unit.setLatitude(request.latitude());
        unit.setLongitude(request.longitude());

        unit.setPhoneNumber(request.phoneNumber());
        unit.setEmail(request.email());
        unit.setVerificationInfo(request.verificationInfo());

        unit.setApprovalStatus(ApprovalStatus.PENDING);

        return medicalUnitRepo.save(unit);
    }
}
