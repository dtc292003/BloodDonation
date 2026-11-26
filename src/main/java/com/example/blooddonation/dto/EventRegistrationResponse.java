package com.example.blooddonation.dto;

import com.example.blooddonation.enums.CheckInStatus;
import com.example.blooddonation.enums.EventRegistrationStatus;

import java.time.LocalDateTime;

public record EventRegistrationResponse(
        Integer registrationId,
        Integer userId,
        Integer eventId,
        String eventName,
        Integer SlotId,
        LocalDateTime slotStartAt,
        LocalDateTime slotEndAt,
        EventRegistrationStatus registrationStatus,
        CheckInStatus checkInStatus,
        String qrCodeToken,
        LocalDateTime registeredAt,
        LocalDateTime checkedInAt,

        LocalDateTime cancelledAt,

        LocalDateTime updatedAt

) {
}