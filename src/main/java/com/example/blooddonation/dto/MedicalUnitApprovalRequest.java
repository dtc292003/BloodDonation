
package com.example.blooddonation.dto;

import com.example.blooddonation.enums.MedicalUnitReviewDecision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedicalUnitApprovalRequest(

        @NotNull
        MedicalUnitReviewDecision decision,

        @NotBlank
        String reason

) {}
