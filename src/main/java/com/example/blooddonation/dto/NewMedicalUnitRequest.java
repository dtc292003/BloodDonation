package com.example.blooddonation.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record NewMedicalUnitRequest(

        @NotBlank
        @Size(max = 150)
        String unitName,

        @NotBlank
        @Size(max = 30)
        String unitType,

        @NotBlank
        @Size(max = 255)
        String address,

        String province,
        String district,
        String ward,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal longitude,

        @Size(max = 20)
        String phoneNumber,

        @NotBlank
        String verificationInfo

) {}