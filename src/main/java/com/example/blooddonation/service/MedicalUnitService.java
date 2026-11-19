
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
