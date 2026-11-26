package com.example.blooddonation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateEventRegistrationRequest(
        @NotNull(message = "Vui lòng chọn sự kiện hiến máu")
        @Positive(message = "EventId phải lớn hơn 0")
        Integer eventId,

        @NotNull(message = "Vui lòng chọn giờ tham gia")
        @Positive(message = "SlotId phải lớn hơn 0")
        Integer slotId
) {
}
