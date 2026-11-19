
package com.example.blooddonation.dto;

import com.example.blooddonation.enums.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record MedicalRegisterRequest(

        @NotBlank
        String fullName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        String phoneNumber,

        @Past
        LocalDate dateOfBirth,

        @NotNull
        Gender gender,

        String address,

        // Đăng ký vào đơn vị đã tồn tại
        Integer medicalUnitId,

        // Đăng ký đơn vị y tế mới
        @Valid
        NewMedicalUnitRequest newMedicalUnit

) {}
