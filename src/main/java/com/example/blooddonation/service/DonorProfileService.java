
package com.example.blooddonation.service;

import com.example.blooddonation.dto.*;
import com.example.blooddonation.entity.DonorProfile;
import com.example.blooddonation.entity.HealthProfile;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.*;
import com.example.blooddonation.exception.DuplicateResourceException;
import com.example.blooddonation.exception.ResourceNotFoundException;
import com.example.blooddonation.repository.DonorProfileRepository;
import com.example.blooddonation.repository.HealthProfileRepository;
import com.example.blooddonation.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class DonorProfileService {

    private final DonorProfileRepository donorProfileRepo;
    private final HealthProfileRepository healthProfileRepo;
    private final UserRepository userRepo;

    // Create DonorProfile
    public DonorProfileResponse createDonorProfile(CreateDonorProfileRequest request) {
        User user = getCurrentUser();
        if (donorProfileRepo.existsByUser_UserId(user.getUserId())) {
            throw new DuplicateResourceException(
                    "Người dùng đã có hồ sơ người hiến"
            );
        }
        if (request == null) {
            throw badRequest("Dữ liệu hồ sơ không được để trống");
        }
        validatePositive(request.heightCm(), "Chiều cao");
        validatePositive(request.weightKg(), "Cân nặng");

        DonorProfile donor = new DonorProfile();
        donor.setUser(user);
        donor.setAboGroup(request.aboGroup());

        donor.setRhFactor(
                request.rhFactor() == null
                        ? RhFactor.UNKNOWN
                        : request.rhFactor()
        );

        boolean hasBloodType =
                donor.getAboGroup() != null
                        || donor.getRhFactor() != RhFactor.UNKNOWN;

        if (hasBloodType) {
            donor.setBloodTypeSource(BloodTypeSource.SELF_REPORTED);
            donor.setBloodTypeUpdatedAt(LocalDateTime.now());
        }

        donor.setAvailabilityStatus(
                DonorAvailabilityStatus.UNAVAILABLE
        );

        DonorProfile savedDonor = donorProfileRepo.saveAndFlush(donor);

        // HealthProfile
        HealthProfile health = new HealthProfile();
        health.setDonorProfile(savedDonor);
        health.setHeightCm(request.heightCm());
        health.setWeightKg(request.weightKg());
        health.setMedicalHistory(request.medicalHistory());
        health.setAllergies(request.allergies());
        health.setCurrentMedication(request.currentMedication());
        health.setHealthNotes(request.healthNotes());

        healthProfileRepo.saveAndFlush(health);

        return toDonorResponse(savedDonor);
    }

    // View DonorProfile
    @Transactional(readOnly = true)
    public DonorProfileResponse getMyDonorProfile() {
        DonorProfile donor = getMyDonor();
        return toDonorResponse(donor);
    }

    // View HealthProfile
    @Transactional(readOnly = true)
    public HealthProfileResponse getMyHealthProfile() {
        DonorProfile donor = getMyDonor();
        HealthProfile health = getHealthProfile(donor);

        return toHealthResponse(health);
    }

    // Update DonorProfile
    public DonorProfileResponse updateDonorProfile(
            UpdateDonorProfileRequest request
    ) {
        DonorProfile donor = getMyDonor();

        if (request == null || (
                request.aboGroup() == null
                        && request.rhFactor() == null
                        && request.availabilityStatus() == null
        )) {
            throw badRequest("Không có dữ liệu cần cập nhật");
        }

        boolean bloodChanged = false;

        if (request.aboGroup() != null
                && request.aboGroup() != donor.getAboGroup()) {

            donor.setAboGroup(request.aboGroup());
            bloodChanged = true;
        }

        if (request.rhFactor() != null
                && request.rhFactor() != donor.getRhFactor()) {

            donor.setRhFactor(request.rhFactor());
            bloodChanged = true;
        }

        if (bloodChanged) {
            boolean hasBloodType =
                    donor.getAboGroup() != null
                            || donor.getRhFactor() != RhFactor.UNKNOWN;

            donor.setBloodTypeSource(
                    hasBloodType
                            ? BloodTypeSource.SELF_REPORTED
                            : BloodTypeSource.UNKNOWN
            );

            donor.setBloodTypeUpdatedAt(
                    hasBloodType ? LocalDateTime.now() : null
            );
        }

        if (request.availabilityStatus() != null) {
            donor.setAvailabilityStatus(
                    request.availabilityStatus()
            );
        }

        donorProfileRepo.saveAndFlush(donor);

        return toDonorResponse(donor);
    }

    // Update HealthProfile
    public HealthProfileResponse updateHealthProfile(
            UpdateHealthProfileRequest request
    ) {
        DonorProfile donor = getMyDonor();
        HealthProfile health = getHealthProfile(donor);

        if (request == null || (
                request.heightCm() == null
                        && request.weightKg() == null
                        && request.medicalHistory() == null
                        && request.allergies() == null
                        && request.currentMedication() == null
                        && request.healthNotes() == null
        )) {
            throw badRequest("Không có dữ liệu cần cập nhật");
        }

        if (request.heightCm() != null) {
            validatePositive(request.heightCm(), "Chiều cao");
            health.setHeightCm(request.heightCm());
        }

        if (request.weightKg() != null) {
            validatePositive(request.weightKg(), "Cân nặng");
            health.setWeightKg(request.weightKg());
        }

        if (request.medicalHistory() != null) {
            health.setMedicalHistory(request.medicalHistory());
        }

        if (request.allergies() != null) {
            health.setAllergies(request.allergies());
        }

        if (request.currentMedication() != null) {
            health.setCurrentMedication(request.currentMedication());
        }

        if (request.healthNotes() != null) {
            health.setHealthNotes(request.healthNotes());
        }

        healthProfileRepo.saveAndFlush(health);

        return toHealthResponse(health);
    }

    // Get User By JWT
    private User getCurrentUser() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập"
            );
        }

        Integer userId;
        try {
            userId = Integer.valueOf(
                    jwtAuth.getToken().getSubject()
            );
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ"
            );
        }

        User user = userRepo.findByIdWithRole(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy người dùng"
                        )
                );

        if (user.getRole() == null
                || user.getRole().getRoleName() != RoleName.USER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chỉ tài khoản USER được sử dụng chức năng này"
            );
        }

        if (user.getUserStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản chưa đủ điều kiện hoạt động"
            );
        }

        return user;
    }

    // 7. Get DonorProfile of User
    private DonorProfile getMyDonor() {
        User user = getCurrentUser();

        return donorProfileRepo
                .findByUser_UserId(user.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bạn chưa đăng ký làm người hiến máu"
                        )
                );
    }

    // Get HealthProfile
    private HealthProfile getHealthProfile(DonorProfile donor) {
        return healthProfileRepo
                .findByDonorProfile_DonorProfileId(
                        donor.getDonorProfileId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy hồ sơ sức khỏe"
                        )
                );
    }

    // Change DonorProfile Entity to Respone dto
    private DonorProfileResponse toDonorResponse(
            DonorProfile donor
    ) {
        return new DonorProfileResponse(
                donor.getDonorProfileId(),
                donor.getUser().getUserId(),
                donor.getAboGroup(),
                donor.getRhFactor(),
                donor.getBloodTypeSource(),
                donor.getBloodTypeUpdatedAt(),
                donor.getAvailabilityStatus(),
                donor.getLatitude(),
                donor.getLongitude(),
                donor.getLocationUpdatedAt(),
                donor.getCreatedAt(),
                donor.getUpdatedAt()
        );
    }

    // Change HealthProfile to HealthRespone Dto
    private HealthProfileResponse toHealthResponse(
            HealthProfile health
    ) {
        return new HealthProfileResponse(
                health.getHealthProfileId(),
                health.getDonorProfile().getDonorProfileId(),
                health.getHeightCm(),
                health.getWeightKg(),
                health.getMedicalHistory(),
                health.getAllergies(),
                health.getCurrentMedication(),
                health.getHealthNotes(),
                health.getUpdatedAt()
        );
    }

    // Check value
    private void validatePositive(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null || value.signum() <= 0) {
            throw badRequest(
                    fieldName + " phải lớn hơn 0"
            );
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
