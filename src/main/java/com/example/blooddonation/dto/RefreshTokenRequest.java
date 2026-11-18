package com.example.blooddonation.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "RefreshToken không được để trống")
        String refreshToken
) {}
