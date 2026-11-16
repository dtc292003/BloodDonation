package com.example.blooddonation.controller;

import com.example.blooddonation.dto.RegisterRequest;
import com.example.blooddonation.dto.RegisterResponse;
import com.example.blooddonation.dto.ResendOtpRequest;
import com.example.blooddonation.dto.VerifyEmailRequest;
import com.example.blooddonation.service.AuthService;

import com.example.blooddonation.service.EmailOtpService;
import com.example.blooddonation.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailOtpService emailOtpService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String,String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request){
        emailOtpService.verifyEmail(request);

        return ResponseEntity.ok(Map.of("message", "Xác minh email thành công"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
    ) {

        emailOtpService.resendOtp(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Nếu email đang chờ xác minh và đủ điều kiện, " +
                                "hệ thống sẽ gửi mã OTP mới."
                )
        );
    }

}
