
package com.example.blooddonation.controller;

import com.example.blooddonation.dto.CreateEventRegistrationRequest;
import com.example.blooddonation.dto.EventRegistrationResponse;
import com.example.blooddonation.service.EventRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-registrations")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    // USER đăng ký tham gia sự kiện hiến máu
    @PostMapping
    public ResponseEntity<EventRegistrationResponse> register(@Valid @RequestBody CreateEventRegistrationRequest request) {
        EventRegistrationResponse response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //USER xem lịch sử đăng ký sự kiện
    @GetMapping("/me")
    public ResponseEntity<List<EventRegistrationResponse>> getMyRegistrations() {

        return ResponseEntity.ok(registrationService.getMyRegistrations());
    }

    // USER hủy đăng ký sự kiện
    @PatchMapping("/{registrationId}/cancel")
    public ResponseEntity<EventRegistrationResponse>cancelRegistration(@PathVariable Integer registrationId) {
        return ResponseEntity.ok(registrationService.cancelRegistration(registrationId));
    }
}
