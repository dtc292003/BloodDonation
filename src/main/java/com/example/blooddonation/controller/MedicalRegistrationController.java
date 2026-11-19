
package com.example.blooddonation.controller;

import com.example.blooddonation.dto.MedicalRegisterRequest;
import com.example.blooddonation.dto.MedicalRegisterResponse;
import com.example.blooddonation.service.MedicalRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MedicalRegistrationController {

    private final MedicalRegistrationService medicalRegistrationService;

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
}
