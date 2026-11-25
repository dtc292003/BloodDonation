
package com.example.blooddonation.dto;

import com.example.blooddonation.enums.ABOGroup;
import com.example.blooddonation.enums.BloodTypeSource;
import com.example.blooddonation.enums.DonorAvailabilityStatus;
import com.example.blooddonation.enums.RhFactor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DonorProfileResponse(

        Integer donorProfileId,
        Integer userId,

        ABOGroup aboGroup,
        RhFactor rhFactor,

        BloodTypeSource bloodTypeSource,
        LocalDateTime bloodTypeUpdatedAt,

        DonorAvailabilityStatus availabilityStatus,

        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime locationUpdatedAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}
