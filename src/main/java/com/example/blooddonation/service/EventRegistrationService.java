
package com.example.blooddonation.service;

import com.example.blooddonation.dto.CreateEventRegistrationRequest;
import com.example.blooddonation.dto.EventRegistrationResponse;
import com.example.blooddonation.entity.*;
import com.example.blooddonation.enums.*;
import com.example.blooddonation.exception.DuplicateResourceException;
import com.example.blooddonation.exception.ResourceNotFoundException;
import com.example.blooddonation.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private final EventRegistrationRepository registrationRepo;
    private final DonationEventRepository eventRepo;
    private final EventTimeSlotRepository slotRepo;
    private final DonorProfileRepository donorRepo;
    private final HealthProfileRepository healthRepo;
    private final UserRepository userRepo;
    private final DonationCooldownService cooldownService;

    // Đăng ký tham gia sự kiện
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EventRegistrationResponse register(
            CreateEventRegistrationRequest request
    ) {
        if (request == null
                || request.eventId() == null
                || request.slotId() == null
                || request.eventId() <= 0
                || request.slotId() <= 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin sự kiện hoặc khung giờ không hợp lệ"
            );
        }

        User user = getCurrentUser();

        // Khóa sự kiện trong transaction để bảo vệ sức chứa
        DonationEvent event = eventRepo
                .findByIdForUpdate(request.eventId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy sự kiện hiến máu"
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        validateEvent(event, now);

        // Tìm khung giờ thuộc đúng sự kiện
        EventTimeSlot slot = slotRepo
                .findBySlotIdAndEvent_EventId(
                        request.slotId(),
                        event.getEventId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy khung giờ thuộc sự kiện"
                        )
                );

        validateSlot(slot, event, now);

        // Kiểm tra hồ sơ người hiến
        DonorProfile donor = donorRepo
                .findByUser_UserId(user.getUserId())
                .orElseThrow(() ->
                        error(
                                HttpStatus.CONFLICT,
                                "Vui lòng tạo hồ sơ người hiến trước khi đăng ký"
                        )
                );

        HealthProfile health = healthRepo
                .findByDonorProfile_DonorProfileId(
                        donor.getDonorProfileId()
                )
                .orElseThrow(() ->
                        error(
                                HttpStatus.CONFLICT,
                                "Vui lòng hoàn thành hồ sơ sức khỏe"
                        )
                );

        if (health.getHeightCm() == null
                || health.getWeightKg() == null
                || health.getHeightCm().signum() <= 0
                || health.getWeightKg().signum() <= 0) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Hồ sơ sức khỏe chưa đầy đủ"
            );
        }

        // Kiểm tra tuổi tại ngày dự kiến hiến
        if (user.getDateOfBirth() == null) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Vui lòng cập nhật ngày sinh"
            );
        }

        int age = Period.between(
                user.getDateOfBirth(),
                slot.getStartAt().toLocalDate()
        ).getYears();

        if (age < 18 || age > 60) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Độ tuổi chưa đáp ứng điều kiện đăng ký hiến máu"
            );
        }

        // Kiểm tra khoảng nghỉ hiến máu
        var cooldown = cooldownService.checkCooldown(
                donor.getDonorProfileId()
        );

        if (cooldown.requiresMedicalReview()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Lịch sử hiến máu cần nhân viên y tế kiểm tra"
            );
        }

        // So sánh với ngày diễn ra khung giờ,
        // không chỉ so sánh với thời điểm đăng ký.
        if (cooldown.nextAllowedAt() != null
                && slot.getStartAt().isBefore(
                cooldown.nextAllowedAt()
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Chưa đủ khoảng nghỉ hiến máu tại ngày tham gia"
            );
        }

        // Kiểm tra đăng ký đã tồn tại
        Optional<EventRegistration> existing =
                registrationRepo.findByEvent_EventIdAndUser_UserId(
                        event.getEventId(),
                        user.getUserId()
                );

        if (existing.isPresent()) {
            EventRegistration old = existing.get();

            if (old.getRegistrationStatus()
                    != EventRegistrationStatus.CANCELLED) {
                throw new DuplicateResourceException(
                        "Bạn đã có đăng ký trong sự kiện này"
                );
            }

            if (old.getCheckInStatus() == CheckInStatus.CHECKED_IN) {
                throw error(
                        HttpStatus.CONFLICT,
                        "Không thể đăng ký lại lượt đã điểm danh"
                );
            }
        }

        // Kiểm tra sức chứa sự kiện
        long eventCount =
                registrationRepo.countByEvent_EventIdAndRegistrationStatus(
                        event.getEventId(),
                        EventRegistrationStatus.REGISTERED
                );

        if (event.getCapacity() == null
                || event.getCapacity() <= 0
                || eventCount >= event.getCapacity()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Sự kiện đã hết chỗ"
            );
        }

        // Kiểm tra sức chứa khung giờ
        long slotCount =
                registrationRepo.countBySlot_SlotIdAndRegistrationStatus(
                        slot.getSlotId(),
                        EventRegistrationStatus.REGISTERED
                );

        if (slot.getMaxParticipants() == null
                || slot.getMaxParticipants() <= 0
                || slotCount >= slot.getMaxParticipants()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Khung giờ đã hết chỗ"
            );
        }

        // Nếu từng hủy thì tái sử dụng bản ghi cũ
        EventRegistration registration = existing
                .orElseGet(EventRegistration::new);

        registration.setEvent(event);
        registration.setSlot(slot);
        registration.setUser(user);

        registration.setRegistrationStatus(
                EventRegistrationStatus.REGISTERED
        );
        registration.setCheckInStatus(
                CheckInStatus.NOT_CHECKED_IN
        );

        registration.setQrCodeToken(
                UUID.randomUUID().toString()
        );

        registration.setRegisteredAt(now);
        registration.setCheckedInAt(null);
        registration.setCheckedInByUser(null);
        registration.setCancelledAt(null);

        EventRegistration saved =
                registrationRepo.saveAndFlush(registration);

        return toResponse(saved);
    }

    // Kiểm tra sự kiện
    private void validateEvent(
            DonationEvent event,
            LocalDateTime now
    ) {
        if (event.getApprovalStatus()
                != EventApprovalStatus.APPROVED
                || event.getEventStatus() != EventStatus.OPEN) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Sự kiện chưa được phép nhận đăng ký"
            );
        }

        if (event.getStartDateTime() == null
                || event.getEndDateTime() == null
                || !event.getEndDateTime().isAfter(
                event.getStartDateTime()
        )
                || !now.isBefore(event.getStartDateTime())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Sự kiện không còn thời gian đăng ký hợp lệ"
            );
        }

        if (event.getRegistrationOpenAt() != null
                && now.isBefore(event.getRegistrationOpenAt())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Sự kiện chưa mở đăng ký"
            );
        }

        if (event.getRegistrationCloseAt() != null
                && !now.isBefore(event.getRegistrationCloseAt())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Sự kiện đã đóng đăng ký"
            );
        }
    }

    //Kiểm tra khung giờ
    private void validateSlot(
            EventTimeSlot slot,
            DonationEvent event,
            LocalDateTime now
    ) {
        if (slot.getSlotStatus() != SlotStatus.OPEN) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Khung giờ không nhận đăng ký"
            );
        }

        if (slot.getStartAt() == null
                || slot.getEndAt() == null
                || !slot.getEndAt().isAfter(slot.getStartAt())
                || !now.isBefore(slot.getStartAt())
                || slot.getStartAt().isBefore(
                event.getStartDateTime()
        )
                || slot.getEndAt().isAfter(
                event.getEndDateTime()
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Thời gian khung giờ không hợp lệ"
            );
        }
    }

    // Lấy user đang đăng nhập
    private User getCurrentUser() {
        Authentication auth =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập"
            );
        }

        Integer userId;
        try {
            userId = Integer.valueOf(
                    jwtAuth.getToken().getSubject()
            );
        } catch (NumberFormatException ex) {
            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ"
            );
        }

        User user = userRepo.findByIdWithRole(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy người dùng"
                        )
                );

        if (user.getRole() == null
                || user.getRole().getRoleName() != RoleName.USER) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Chỉ USER được đăng ký sự kiện hiến máu"
            );
        }

        if (user.getUserStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản chưa đủ điều kiện hoạt động"
            );
        }

        return user;
    }

    // Chuyển entity sang dto
    private EventRegistrationResponse toResponse(
            EventRegistration registration
    ) {
        return new EventRegistrationResponse(
                registration.getRegistrationId(),
                registration.getUser().getUserId(),
                registration.getEvent().getEventId(),
                registration.getEvent().getEventName(),
                registration.getSlot().getSlotId(),
                registration.getSlot().getStartAt(),
                registration.getSlot().getEndAt(),
                registration.getRegistrationStatus(),
                registration.getCheckInStatus(),
                registration.getQrCodeToken(),
                registration.getRegisteredAt(),
                registration.getCheckedInAt(),
                registration.getCancelledAt(),
                registration.getUpdatedAt()
        );
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message
    ) {
        return new ResponseStatusException(status, message);
    }

    @Transactional
    public EventRegistrationResponse cancelRegistration(
            Integer registrationId
    ) {
        // Xác định user đang đăng nhập
        User user = getCurrentUser();

        // Tìm đăng ký
        EventRegistration registration = registrationRepo
                .findById(registrationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy đăng ký sự kiện"
                        )
                );

        // Kiểm tra quyền sở hữu
        if (!registration.getUser().getUserId()
                .equals(user.getUserId())) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy đăng ký này"
            );
        }

        // Kiểm tra trạng thái đăng ký
        if (registration.getRegistrationStatus()
                != EventRegistrationStatus.REGISTERED) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Đăng ký không ở trạng thái có thể hủy"
            );
        }

        // Không cho hủy khi đã check-in
        if (registration.getCheckInStatus()
                == CheckInStatus.CHECKED_IN) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Không thể hủy đăng ký đã điểm danh"
            );
        }

        // Không cho hủy khi khung giờ đã bắt đầu
        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(registration.getSlot().getStartAt())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Đã quá thời gian cho phép hủy đăng ký"
            );
        }

        // Cập nhật trạng thái
        registration.setRegistrationStatus(
                EventRegistrationStatus.CANCELLED
        );
        registration.setCancelledAt(now);

        // Vô hiệu hóa QR cũ bằng cách thay token
        registration.setQrCodeToken(
                UUID.randomUUID().toString()
        );

        // Lưu Database
        EventRegistration saved =
                registrationRepo.saveAndFlush(registration);

        return toResponse(saved);
    }

    //Lấy danh sách sự kiện User đăng ký
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> getMyRegistrations(){
        //Lấy User đang đang nhập
        User user = getCurrentUser();

        //Xem lịch sử đăng ký
        List<EventRegistration> registrations = registrationRepo
                .findByUser_UserIdOrderByRegisteredAtDesc(
                        user.getUserId()
                );
        // Chuyển entity sang dto
        return registrations.stream()
                .map(this::toResponse)
                .toList();
    }
}
