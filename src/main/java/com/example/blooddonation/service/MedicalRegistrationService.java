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

    // API JSON cũ: giữ nguyên đăng ký đơn vị có sẵn hoặc đơn vị mới.
    @Transactional
    public MedicalRegisterResponse register(MedicalRegisterRequest request) {
        return registerInternal(request, null);
    }

    // chỉ đăng ký đơn vị y tế mới kèm tài liệu.
    @Transactional
    public MedicalRegisterResponse registerWithDocument(
            MedicalRegisterRequest request,
            MultipartFile document
    ) {
        if (request.medicalUnitId() != null || request.newMedicalUnit() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tải giấy tờ chỉ áp dụng khi đăng ký đơn vị y tế mới"
            );
        }

        if (document == null || document.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng tải giấy tờ xác minh"
            );
        }

        return registerInternal(request, document);
    }

    // Logic đăng ký dùng chung; hai phương thức public mở transaction.
    private MedicalRegisterResponse registerInternal(
            MedicalRegisterRequest request,
            MultipartFile document
    ) {
        // 1. Chỉ được chọn một phương án đăng ký đơn vị.
        boolean hasUnitId = request.medicalUnitId() != null;
        boolean hasNewUnit = request.newMedicalUnit() != null;

        if (hasUnitId == hasNewUnit) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chọn đơn vị có sẵn hoặc khai báo đơn vị mới"
            );
        }

        // 2. Chuẩn hóa email và kiểm tra trùng.
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepo.existsByEmail(email)) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }

        // BCrypt chỉ xử lý tối đa 72 byte mật khẩu.
        int passwordBytes = request.password()
                .getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu vượt quá giới hạn BCrypt"
            );
        }

        // 3. Gán Role MEDICAL ở Backend, không nhận Role từ client.
        Role role = roleRepo.findByRoleName(RoleName.MEDICAL)
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa cấu hình Role MEDICAL"
                ));

        // 4. Chọn đơn vị đã duyệt hoặc tạo đơn vị mới ở trạng thái PENDING.
        MedicalUnit unit;

        if (hasUnitId) {
            unit = medicalUnitRepo.findById(request.medicalUnitId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy đơn vị y tế"
                    ));

            if (unit.getApprovalStatus() != ApprovalStatus.APPROVED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn vị y tế chưa được phê duyệt"
                );
            }
        } else {
            unit = createMedicalUnit(request.newMedicalUnit());
        }

        // 5. Tạo tài khoản MEDICAL, chờ xác minh email.
        User user = new User();

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
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

        // 6. Nếu có file, lưu tài liệu của đơn vị mới trong cùng transaction.
        if (document != null) {
            documentService.save(unit, document);
        }

        // 7. Tạo và gửi OTP bằng hệ thống hiện có.
        emailOtpService.createAndSendOtp(savedUser.getUserId());

        return new MedicalRegisterResponse(
                savedUser.getUserId(),
                unit.getMedicalUnitId(),
                savedUser.getUserStatus(),
                "Đăng ký thành công. Vui lòng xác minh email."
        );
    }

    private MedicalUnit createMedicalUnit(NewMedicalUnitRequest request) {
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
