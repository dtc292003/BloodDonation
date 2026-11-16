package com.example.blooddonation.service;

public record OtpMailEvent(
        String toEmail,
        String otp
) {}