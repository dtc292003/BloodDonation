package com.example.blooddonation.controller;

import com.example.blooddonation.dto.*;
import com.example.blooddonation.service.DonorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donor-profiles")
@RequiredArgsConstructor
public class DonorProfileController {
    private final DonorProfileService donorProfileService;

    // Create DonorProfile
    @PostMapping
    public ResponseEntity<DonorProfileResponse> createDonorProfile(@Valid @RequestBody CreateDonorProfileRequest request){
        DonorProfileResponse response = donorProfileService.createDonorProfile(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //View MyProfile
    @GetMapping("/me")
    public ResponseEntity<DonorProfileResponse> getMyDonorProfile(){
        return ResponseEntity.ok(donorProfileService.getMyDonorProfile());
    }

    //Update DonorProfile
    @PatchMapping("/me")
    public ResponseEntity<DonorProfileResponse> updateDonorProfile(
            @Valid @RequestBody UpdateDonorProfileRequest request
    ) {
        return ResponseEntity.ok(
                donorProfileService.updateDonorProfile(request)
        );
    }

    //View MyHealthProfile
    @GetMapping("/me/health")
    public ResponseEntity<HealthProfileResponse> getMyHealthProfile(){
        return ResponseEntity.ok(donorProfileService.getMyHealthProfile());
    }
    //Update HealthProfile
    @PatchMapping("/me/health")
    public ResponseEntity<HealthProfileResponse> updateHealthProfile(
            @Valid @RequestBody UpdateHealthProfileRequest request
    ) {
        return ResponseEntity.ok(
                donorProfileService.updateHealthProfile(request)
        );
    }
}
