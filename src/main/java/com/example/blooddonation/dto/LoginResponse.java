package com.example.blooddonation.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Integer userId,
        String fullName,
        String email,
        String role
) {}
