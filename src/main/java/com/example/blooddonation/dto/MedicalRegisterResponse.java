package com.example.blooddonation.dto;

import com.example.blooddonation.enums.UserStatus;

public record MedicalRegisterResponse(
        Integer userId,
        Integer medicalUnitId,
        UserStatus userStatus,
        String message
) {
}
