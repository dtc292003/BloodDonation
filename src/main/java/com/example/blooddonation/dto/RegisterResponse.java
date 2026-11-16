package com.example.blooddonation.dto;

import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;

public record RegisterResponse(
        Integer userId,
        String fullName,
        String email,
        RoleName roleName,
        UserStatus userStatus,
        String message
) {
}
