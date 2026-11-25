
package com.example.blooddonation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record UpdateHealthProfileRequest(
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
