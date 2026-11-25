package com.example.blooddonation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(
                regexp = "\\d{6}",
                message = "Mã OTP phải gồm đúng 6 chữ số"
        )
        String otp,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(
                min = 8,
                max = 64,
                message = "Mật khẩu phải từ 8 đến 64 ký tự"
        )
        String newPassword
) {}
