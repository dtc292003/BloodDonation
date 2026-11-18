package com.example.blooddonation.controller;

import com.example.blooddonation.dto.*;
import com.example.blooddonation.repository.RefreshTokenRepository;
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
            @Valid @RequestBody ResendOtpRequest request
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =    authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ){
        return ResponseEntity.ok(authService.refresh(request));
    }
    @PostMapping("/logout")
    public  ResponseEntity<Map<String,String>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ){
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

}
