package com.example.blooddonation.dto;

import com.example.blooddonation.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserUpdateRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String fullName,
        String phoneNumber,
        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate dateOfBirth,
        Gender gender,
        String avatarUrl,
        String address
) {}
