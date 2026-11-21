
package com.example.blooddonation.service;

import com.example.blooddonation.dto.MedicalUnitResponse;
import com.example.blooddonation.dto.MedicalUnitApprovalRequest;
import com.example.blooddonation.entity.*;
import com.example.blooddonation.enums.*;
import com.example.blooddonation.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalUnitService {

    private final MedicalUnitRepository medicalUnitRepo;
    private final MedicalUnitReviewRepository medicalUnitReviewRepo;
    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshTokenRepo;

    @Transactional(readOnly = true)
    public List<MedicalUnitResponse> getPendingUnits() {

        return medicalUnitRepo
                .findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(MedicalUnitResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicalUnitResponse getById(Integer id) {

        MedicalUnit unit = medicalUnitRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn vị y tế"
                ));

        return MedicalUnitResponse.from(unit);
    }

    @Transactional
    public MedicalUnitResponse reviewUnit(
            Integer medicalUnitId,
            Integer adminId,
            MedicalUnitApprovalRequest request
    ) {

        // Check By Admin
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Không tìm thấy tài khoản ADMIN"
                ));

        if (admin.getRole() == null ||
                admin.getRole().getRoleName() != RoleName.ADMIN ||
                admin.getUserStatus() != UserStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Chỉ ADMIN đang hoạt động được xét duyệt"
            );
        }

        // Find MedicalUnit
        MedicalUnit unit = medicalUnitRepo
                .findById(medicalUnitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn vị y tế"
                ));

        ApprovalStatus oldStatus = unit.getApprovalStatus();

        // Change Status
        ApprovalStatus newStatus = switch (request.decision()) {

            case APPROVE -> {
                requireStatus(oldStatus, ApprovalStatus.PENDING);
                yield ApprovalStatus.APPROVED;
            }

            case REJECT -> {
                requireStatus(oldStatus, ApprovalStatus.PENDING);
                yield ApprovalStatus.REJECTED;
            }

            case SUSPEND -> {
                requireStatus(oldStatus, ApprovalStatus.APPROVED);
                yield ApprovalStatus.SUSPENDED;
            }

            case REACTIVATE -> {
                requireStatus(oldStatus, ApprovalStatus.SUSPENDED);
                yield ApprovalStatus.APPROVED;
            }
        };

        // Tài khoản MEDICAL gắn với cơ sở y tế
        List<User> medicalAccounts =
                userRepo.findByMedicalUnit_MedicalUnitId(medicalUnitId);

// Khi ADMIN duyệt hoặc từ chối cơ sở,
// đồng thời cập nhật tài khoản MEDICAL
        if (request.decision() == MedicalUnitReviewDecision.APPROVE
                || request.decision() == MedicalUnitReviewDecision.REJECT) {

            // Mỗi cơ sở chỉ được có một tài khoản MEDICAL
            if (medicalAccounts.size() != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cơ sở phải có đúng một tài khoản MEDICAL"
                );
            }

            User medicalUser = medicalAccounts.get(0);

            if (medicalUser.getRole() == null
                    || medicalUser.getRole().getRoleName() != RoleName.MEDICAL) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tài khoản liên kết không có quyền MEDICAL"
                );
            }

            if (request.decision() == MedicalUnitReviewDecision.APPROVE) {

                // Chỉ duyệt tài khoản đã xác minh email
                if (!Boolean.TRUE.equals(medicalUser.getEmailVerified())
                        || medicalUser.getUserStatus() != UserStatus.PENDING_APPROVAL) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Tài khoản MEDICAL chưa hoàn tất xác minh OTP"
                    );
                }

                medicalUser.setUserStatus(UserStatus.ACTIVE);

            } else {

                if (medicalUser.getUserStatus() != UserStatus.PENDING_EMAIL
                        && medicalUser.getUserStatus() != UserStatus.PENDING_APPROVAL) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Tài khoản không ở trạng thái có thể từ chối"
                    );
                }

                medicalUser.setUserStatus(UserStatus.REJECTED);
            }

            medicalUser.setUpdatedAt(LocalDateTime.now());

            userRepo.save(medicalUser);
        }
        // Update Status
        unit.setApprovalStatus(newStatus);
        medicalUnitRepo.save(unit);

        // History
        MedicalUnitReview review = new MedicalUnitReview();

        review.setMedicalUnit(unit);
        review.setReviewedByAdmin(admin);
        review.setDecision(request.decision());
        review.setReason(request.reason().trim());
        review.setReviewedAt(LocalDateTime.now());

        medicalUnitReviewRepo.save(review);

        // Revoke all user
        List<User> members =
                userRepo.findByMedicalUnit_MedicalUnitId(
                        medicalUnitId
                );

        Instant revokedTime = Instant.now();

        for (User member : members) {

            member.setTokenVersion(
                    member.getTokenVersion() + 1
            );

            userRepo.save(member);

            refreshTokenRepo.revokeAllByUserId(
                    member.getUserId(),
                    revokedTime
            );
        }

        return MedicalUnitResponse.from(unit);
    }

    private void requireStatus(
            ApprovalStatus current,
            ApprovalStatus expected
    ) {
        if (current != expected) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Trạng thái đơn vị không phù hợp với hành động"
            );
        }
    }
}
