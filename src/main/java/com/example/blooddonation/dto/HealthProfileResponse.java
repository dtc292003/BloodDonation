
package com.example.blooddonation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthProfileResponse(
        Integer healthProfileId,
        Integer donorProfileId,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String medicalHistory,
        String allergies,
        String currentMedication,
        String healthNotes,
        LocalDateTime updatedAt

) {}
