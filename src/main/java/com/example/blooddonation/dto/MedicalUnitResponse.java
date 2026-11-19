
package com.example.blooddonation.dto;

import com.example.blooddonation.entity.MedicalUnit;
import com.example.blooddonation.enums.ApprovalStatus;

public record MedicalUnitResponse(
        Integer medicalUnitId,
        String unitName,
        String unitType,
        String address,
        String province,
        String district,
        String ward,
        String phoneNumber,
        String email,
        String verificationInfo,
        ApprovalStatus approvalStatus
) {
    public static MedicalUnitResponse from(MedicalUnit unit) {
        return new MedicalUnitResponse(
                unit.getMedicalUnitId(),
                unit.getUnitName(),
                unit.getUnitType(),
                unit.getAddress(),
                unit.getProvince(),
                unit.getDistrict(),
                unit.getWard(),
                unit.getPhoneNumber(),
                unit.getEmail(),
                unit.getVerificationInfo(),
                unit.getApprovalStatus()
        );
    }
}
