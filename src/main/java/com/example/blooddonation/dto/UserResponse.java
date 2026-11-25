package com.example.blooddonation.dto;

import com.example.blooddonation.enums.Gender;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


public record UserResponse(
        Integer userId,
        String fullName,
        String email,
        String phoneNumber,
        @DateTimeFormat(pattern = "dd/MM/yyyy")
        LocalDate dateOfBirth,
        String address,
        Gender gender,
        String avatarUrl,
        Boolean emailVerified,
        UserStatus userStatus,
        RoleName roleName
) {
}
