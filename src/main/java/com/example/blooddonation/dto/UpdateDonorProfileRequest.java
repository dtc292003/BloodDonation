package com.example.blooddonation.dto;

import com.example.blooddonation.enums.ABOGroup;
import com.example.blooddonation.enums.DonorAvailabilityStatus;
import com.example.blooddonation.enums.RhFactor;

public record UpdateDonorProfileRequest(
        ABOGroup aboGroup,
        RhFactor rhFactor,
        DonorAvailabilityStatus availabilityStatus
) {}
