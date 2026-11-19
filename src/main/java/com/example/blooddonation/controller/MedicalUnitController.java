package com.example.blooddonation.controller;

import com.example.blooddonation.dto.MedicalUnitApprovalRequest;
import com.example.blooddonation.dto.MedicalUnitResponse;
import com.example.blooddonation.service.MedicalUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-units")
@RequiredArgsConstructor
public class MedicalUnitController {

    private final MedicalUnitService medicalUnitService;

    //Get list Peding
    @GetMapping("/pending")
    public ResponseEntity<List<MedicalUnitResponse>> getPending(){
        return ResponseEntity.ok(medicalUnitService.getPendingUnits());
    }

    //Get detail MedicalUnit

    @GetMapping("/{id}")
    public ResponseEntity<MedicalUnitResponse> getById(@PathVariable Integer id){
        return ResponseEntity.ok(medicalUnitService.getById(id));
    }

    //Approve or reject a medical unit registration

    @PatchMapping("/{id}/approval")
    public ResponseEntity<MedicalUnitResponse> reviewUnit(
            @PathVariable Integer id,
            @Valid @RequestBody MedicalUnitApprovalRequest request,
            @AuthenticationPrincipal Jwt jwt){

        Integer adminId = Integer.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                medicalUnitService.reviewUnit(
                        id,
                        adminId,
                        request
                )
        );
    }
}
