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
import org.springframework.web.multipart.MultipartFile;
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
    private final MedicalVerificationDocumentService documentService;

    @Transactional
    public MedicalRegisterResponse registerWithDocument(
            MedicalRegisterRequest request,
            MultipartFile document
    ) {

        // 1. Bắt buộc tải giấy tờ xác minh
        if (document == null || document.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng tải giấy tờ xác minh cơ sở y tế"
            );
        }

        // 2. Chuẩn hóa email cơ sở
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepo.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email đã được sử dụng"
            );
        }

        // 3. Kiểm tra giới hạn BCrypt
        if (request.password()
                .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu vượt quá giới hạn BCrypt"
            );
        }

        // 4. Lấy Role MEDICAL
        Role role = roleRepo
                .findByRoleName(RoleName.MEDICAL)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Chưa cấu hình Role MEDICAL"
                        )
                );

        // 5. Tạo cơ sở y tế mới
        NewMedicalUnitRequest info =
                request.newMedicalUnit();

        MedicalUnit unit = new MedicalUnit();

        unit.setUnitName(info.unitName().trim());
        unit.setUnitType(info.unitType().trim());
        unit.setAddress(info.address().trim());

        unit.setProvince(info.province());
        unit.setDistrict(info.district());
        unit.setWard(info.ward());

        unit.setLatitude(info.latitude());
        unit.setLongitude(info.longitude());

        unit.setPhoneNumber(info.phoneNumber());

        // Dùng duy nhất email đã đăng ký
        unit.setEmail(email);

        unit.setVerificationInfo(
                info.verificationInfo().trim()
        );

        unit.setApprovalStatus(ApprovalStatus.PENDING);

        MedicalUnit savedUnit =
                medicalUnitRepo.save(unit);

        // 6. Tạo tài khoản MEDICAL
        User user = new User();

        // User.fullName bắt buộc trong database
        // nên sử dụng tên cơ sở
        user.setFullName(info.unitName().trim());

        user.setEmail(email);

        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setPhoneNumber(info.phoneNumber());
        user.setAddress(info.address());

        // Không còn người đại diện
        user.setDateOfBirth(null);
        user.setGender(null);

        user.setRole(role);
        user.setMedicalUnit(savedUnit);

        user.setEmailVerified(false);
        user.setUserStatus(UserStatus.PENDING_EMAIL);

        LocalDateTime now = LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepo.save(user);

        // 7. Lưu giấy tờ xác minh
        documentService.save(savedUnit, document);

        // 8. Gửi OTP đến email cơ sở
        emailOtpService.createAndSendOtp(
                savedUser.getUserId()
        );

        return new MedicalRegisterResponse(
                savedUser.getUserId(),
                savedUnit.getMedicalUnitId(),
                savedUser.getUserStatus(),
                "Đăng ký thành công. Vui lòng xác minh email và chờ ADMIN phê duyệt."
        );
    }
}