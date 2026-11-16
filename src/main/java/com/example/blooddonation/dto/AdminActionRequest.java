package com.example.blooddonation.dto;

import com.example.blooddonation.enums.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminActionRequest(
        @NotNull(message = "Hành động không được để trống")
        ActionType action,

        @NotBlank(message = "Lý do không được để trống")

        String reason
) {}
