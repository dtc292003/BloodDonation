package com.example.blooddonation.dto;

import com.example.blooddonation.enums.ABOGroup;
import com.example.blooddonation.enums.RhFactor;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateDonorProfileRequest(
        ABOGroup aboGroup,
        RhFactor rhFactor,
        @NotNull(message = "Chiều cao không được để trống")
        @DecimalMin(
                value = "0.01",
                message = "Chiều cao phải lớn hơn 0"
        )
        @Digits(
                integer = 3,
                fraction = 2,
                message = "Chiều cao không đúng định dạng"
        )
        BigDecimal heightCm,
        @NotNull(message = "Cân nặng không được để trống")
        @DecimalMin(
                value = "0.01",
                message = "Cân nặng phải lớn hơn 0"
        )
        @Digits(
                integer = 3,
                fraction = 2,
                message = "Cân nặng không đúng định dạng"
        )
        BigDecimal weightKg,
        String medicalHistory,
        String allergies,
        String currentMedication,
        String healthNotes

) {}
