
package com.example.blooddonation.controller;

import com.example.blooddonation.dto.MedicalRegisterRequest;
import com.example.blooddonation.dto.MedicalRegisterResponse;
import com.example.blooddonation.service.MedicalRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MedicalRegistrationController {

    private final MedicalRegistrationService medicalRegistrationService;

    // Đăng ký MEDICAL bằng JSON - giữ nguyên API cũ
    @PostMapping("/register-medical")
    public ResponseEntity<MedicalRegisterResponse> registerMedical(
            @Valid @RequestBody MedicalRegisterRequest request
    ) {
        MedicalRegisterResponse response =
                medicalRegistrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Đăng ký MEDICAL kèm giấy tờ xác minh
    @PostMapping(
            value = "/register-medical-with-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MedicalRegisterResponse> registerMedicalWithDocument(
            @Valid @RequestPart("request")
            MedicalRegisterRequest request,

            @RequestPart("document")
            MultipartFile document
    ) {
        MedicalRegisterResponse response =
                medicalRegistrationService.registerWithDocument(
                        request,
                        document
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
