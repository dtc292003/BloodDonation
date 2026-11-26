
package com.example.blooddonation.service;

import com.example.blooddonation.dto.CreateEventRegistrationRequest;
import com.example.blooddonation.entity.*;
import com.example.blooddonation.enums.*;
import com.example.blooddonation.exception.DuplicateResourceException;
import com.example.blooddonation.repository.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {

    private EventRegistration createRegistration() {

        User user = new User();
        user.setUserId(USER_ID);

        DonationEvent event = new DonationEvent();
        event.setEventId(EVENT_ID);
        event.setEventName("Ngày hội hiến máu");

        EventTimeSlot slot = new EventTimeSlot();
        slot.setSlotId(SLOT_ID);
        slot.setEvent(event);
        slot.setStartAt(LocalDateTime.now().plusDays(2));
        slot.setEndAt(LocalDateTime.now().plusDays(2).plusHours(1));

        EventRegistration registration = new EventRegistration();
        registration.setRegistrationId(10);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setSlot(slot);
        registration.setRegistrationStatus(
                EventRegistrationStatus.REGISTERED
        );
        registration.setCheckInStatus(
                CheckInStatus.NOT_CHECKED_IN
        );
        registration.setQrCodeToken("old-qr-token");
        registration.setRegisteredAt(LocalDateTime.now().minusDays(1));

        return registration;
    }


    @Mock
    private EventRegistrationRepository registrationRepo;

    @Mock
    private DonationEventRepository eventRepo;

    @Mock
    private EventTimeSlotRepository slotRepo;

    @Mock
    private DonorProfileRepository donorRepo;

    @Mock
    private HealthProfileRepository healthRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private DonationCooldownService cooldownService;

    @InjectMocks
    private EventRegistrationService registrationService;

    private static final Integer USER_ID = 23;
    private static final Integer EVENT_ID = 1;
    private static final Integer SLOT_ID = 3;
    private static final Integer DONOR_ID = 5;

    // Giả lập USER đang đăng nhập bằng JWT
    private void mockAuthenticatedUser() {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(String.valueOf(USER_ID))
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(jwt)
                );

        Role role = new Role();
        role.setRoleName(RoleName.USER);

        User user = new User();
        user.setUserId(USER_ID);
        user.setRole(role);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setDateOfBirth(
                java.time.LocalDate.of(2003, 9, 2)
        );

        when(userRepo.findByIdWithRole(USER_ID))
                .thenReturn(Optional.of(user));
    }

    // Giả lập sự kiện và khung giờ hợp lệ
    private EventTimeSlot mockValidEventAndSlot() {

        LocalDateTime now = LocalDateTime.now();

        DonationEvent event = new DonationEvent();
        event.setEventId(EVENT_ID);
        event.setEventName("Ngay hoi hien mau");
        event.setApprovalStatus(EventApprovalStatus.APPROVED);
        event.setEventStatus(EventStatus.OPEN);
        event.setStartDateTime(now.plusDays(2));
        event.setEndDateTime(
                now.plusDays(2).plusHours(6)
        );
        event.setCapacity(100);

        when(eventRepo.findByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        EventTimeSlot slot = new EventTimeSlot();
        slot.setSlotId(SLOT_ID);
        slot.setEvent(event);
        slot.setStartAt(
                now.plusDays(2).plusHours(1)
        );
        slot.setEndAt(
                now.plusDays(2).plusHours(2)
        );
        slot.setMaxParticipants(20);
        slot.setSlotStatus(SlotStatus.OPEN);

        when(slotRepo.findBySlotIdAndEvent_EventId(
                SLOT_ID, EVENT_ID
        )).thenReturn(Optional.of(slot));

        return slot;
    }

    // Giả lập hồ sơ người hiến hợp lệ
    private void mockValidDonorAndHealth() {

        DonorProfile donor = new DonorProfile();
        donor.setDonorProfileId(DONOR_ID);

        when(donorRepo.findByUser_UserId(USER_ID))
                .thenReturn(Optional.of(donor));

        HealthProfile health = new HealthProfile();
        health.setDonorProfile(donor);
        health.setHeightCm(BigDecimal.valueOf(165));
        health.setWeightKg(BigDecimal.valueOf(55));

        when(healthRepo.findByDonorProfile_DonorProfileId(
                DONOR_ID
        )).thenReturn(Optional.of(health));

        var cooldown =
                new DonationCooldownService.CooldownResult(
                        true,
                        false,
                        null,
                        null,
                        null,
                        "Chưa có lịch sử hiến máu"
                );

        when(cooldownService.checkCooldown(DONOR_ID))
                .thenReturn(cooldown);
    }

    // Test 1: USER đăng ký thành công
    @Test
    void registerSuccessfully() {

        mockAuthenticatedUser();
        mockValidEventAndSlot();
        mockValidDonorAndHealth();

        when(registrationRepo
                .findByEvent_EventIdAndUser_UserId(
                        EVENT_ID, USER_ID
                )).thenReturn(Optional.empty());

        when(registrationRepo
                .countByEvent_EventIdAndRegistrationStatus(
                        EVENT_ID,
                        EventRegistrationStatus.REGISTERED
                )).thenReturn(0L);

        when(registrationRepo
                .countBySlot_SlotIdAndRegistrationStatus(
                        SLOT_ID,
                        EventRegistrationStatus.REGISTERED
                )).thenReturn(0L);

        when(registrationRepo.saveAndFlush(
                any(EventRegistration.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        var request = new CreateEventRegistrationRequest(
                EVENT_ID, SLOT_ID
        );

        var result = registrationService.register(request);

        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
        assertEquals(EVENT_ID, result.eventId());
        assertEquals(SLOT_ID, result.SlotId());

        assertEquals(
                EventRegistrationStatus.REGISTERED,
                result.registrationStatus()
        );

        assertEquals(
                CheckInStatus.NOT_CHECKED_IN,
                result.checkInStatus()
        );

        assertNotNull(result.qrCodeToken());
    }

    // Test 2: USER đăng ký trùng sự kiện
    @Test
    void duplicateRegistration() {

        mockAuthenticatedUser();
        mockValidEventAndSlot();
        mockValidDonorAndHealth();

        EventRegistration existing = new EventRegistration();
        existing.setRegistrationStatus(
                EventRegistrationStatus.REGISTERED
        );

        when(registrationRepo
                .findByEvent_EventIdAndUser_UserId(
                        EVENT_ID, USER_ID
                )).thenReturn(Optional.of(existing));

        var request = new CreateEventRegistrationRequest(
                EVENT_ID, SLOT_ID
        );

        assertThrows(
                DuplicateResourceException.class,
                () -> registrationService.register(request)
        );
    }

    // Xóa Authentication sau mỗi bài test
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void eventNotApprovedShouldBeRejected() {

        // Giả lập USER đã đăng nhập
        mockAuthenticatedUser();

        // Tạo sự kiện chưa được ADMIN duyệt
        DonationEvent event = new DonationEvent();
        event.setEventId(EVENT_ID);
        event.setApprovalStatus(EventApprovalStatus.PENDING);
        event.setEventStatus(EventStatus.OPEN);

        when(eventRepo.findByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        CreateEventRegistrationRequest request =
                new CreateEventRegistrationRequest(
                        EVENT_ID,
                        SLOT_ID
                );

        // Kiểm tra Service từ chối đăng ký
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.register(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
    }

    @Test
    void slotAtCapacityShouldBeRejected() {

        // 1. Giả lập USER hợp lệ
        mockAuthenticatedUser();

        // 2. Sự kiện và khung giờ hợp lệ
        mockValidEventAndSlot();

        // 3. Hồ sơ sức khỏe và Cooldown hợp lệ
        mockValidDonorAndHealth();

        // 4. USER chưa đăng ký sự kiện
        when(registrationRepo
                .findByEvent_EventIdAndUser_UserId(
                        EVENT_ID, USER_ID
                )).thenReturn(Optional.empty());

        // 5. Sự kiện vẫn còn chỗ
        when(registrationRepo
                .countByEvent_EventIdAndRegistrationStatus(
                        EVENT_ID,
                        EventRegistrationStatus.REGISTERED
                )).thenReturn(10L);

        // 6. Khung giờ đã đủ 20/20 người
        when(registrationRepo
                .countBySlot_SlotIdAndRegistrationStatus(
                        SLOT_ID,
                        EventRegistrationStatus.REGISTERED
                )).thenReturn(20L);

        CreateEventRegistrationRequest request =
                new CreateEventRegistrationRequest(
                        EVENT_ID,
                        SLOT_ID
                );

        // 7. Kiểm tra Service từ chối đăng ký
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.register(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "Khung giờ đã hết chỗ",
                exception.getReason()
        );
    }

    @Test
    void eventRegistrationClosedShouldBeRejected() {

        // 1. USER hợp lệ
        mockAuthenticatedUser();

        LocalDateTime now = LocalDateTime.now();

        // 2. Tạo sự kiện đã được duyệt
        DonationEvent event = new DonationEvent();
        event.setEventId(EVENT_ID);
        event.setApprovalStatus(EventApprovalStatus.APPROVED);
        event.setEventStatus(EventStatus.OPEN);

        // Sự kiện diễn ra trong tương lai
        event.setStartDateTime(now.plusDays(2));
        event.setEndDateTime(
                now.plusDays(2).plusHours(6)
        );

        // Nhưng thời gian đăng ký đã kết thúc
        event.setRegistrationOpenAt(now.minusDays(5));
        event.setRegistrationCloseAt(now.minusDays(1));

        when(eventRepo.findByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        CreateEventRegistrationRequest request =
                new CreateEventRegistrationRequest(EVENT_ID, SLOT_ID);

        // 3. Kiểm tra Service từ chối đăng ký
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.register(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "Sự kiện đã đóng đăng ký",
                exception.getReason()
        );
    }


    @Test
    void donorNotEnoughCooldownShouldBeRejected() {

        // 1. USER hợp lệ
        mockAuthenticatedUser();

        // 2. Sự kiện và khung giờ hợp lệ
        mockValidEventAndSlot();

        // 3. Giả lập DonorProfile
        DonorProfile donor = new DonorProfile();
        donor.setDonorProfileId(DONOR_ID);

        when(donorRepo.findByUser_UserId(USER_ID))
                .thenReturn(Optional.of(donor));

        // 4. Giả lập HealthProfile hợp lệ
        HealthProfile health = new HealthProfile();
        health.setDonorProfile(donor);
        health.setHeightCm(BigDecimal.valueOf(165));
        health.setWeightKg(BigDecimal.valueOf(55));

        when(healthRepo.findByDonorProfile_DonorProfileId(DONOR_ID))
                .thenReturn(Optional.of(health));

        // 5. Giả lập chưa đủ Cooldown
        LocalDateTime now = LocalDateTime.now();

        var cooldown = new DonationCooldownService.CooldownResult(
                false,
                false,
                now.minusDays(30),
                DonationType.WHOLE_BLOOD,
                now.plusDays(54),
                "Chưa đủ khoảng nghỉ hiến máu"
        );

        when(cooldownService.checkCooldown(DONOR_ID))
                .thenReturn(cooldown);

        CreateEventRegistrationRequest request =
                new CreateEventRegistrationRequest(EVENT_ID, SLOT_ID);

        // 6. Kiểm tra Service từ chối
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.register(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "Chưa đủ khoảng nghỉ hiến máu tại ngày tham gia",
                exception.getReason()
        );
    }

    // Test 7: Hủy đăng ký thành công
    @Test
    void cancelRegistrationSuccessfully() {

        mockAuthenticatedUser();

        EventRegistration registration = createRegistration();

        when(registrationRepo.findById(10))
                .thenReturn(Optional.of(registration));

        when(registrationRepo.saveAndFlush(
                any(EventRegistration.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        var result = registrationService.cancelRegistration(10);

        assertEquals(
                EventRegistrationStatus.CANCELLED,
                result.registrationStatus()
        );

        assertNotNull(result.cancelledAt());
        assertNotEquals("old-qr-token", result.qrCodeToken());
    }

    // Test 8: Không được hủy đăng ký của người khác
    @Test
    void cancelOtherUserRegistrationShouldBeForbidden() {

        mockAuthenticatedUser();

        EventRegistration registration = createRegistration();
        registration.getUser().setUserId(99);

        when(registrationRepo.findById(10))
                .thenReturn(Optional.of(registration));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.cancelRegistration(10)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    // Test 9: Không được hủy sau khi check-in
    @Test
    void cancelCheckedInRegistrationShouldBeRejected() {

        mockAuthenticatedUser();

        EventRegistration registration = createRegistration();
        registration.setCheckInStatus(CheckInStatus.CHECKED_IN);

        when(registrationRepo.findById(10))
                .thenReturn(Optional.of(registration));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.cancelRegistration(10)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    // Test 10: Không được hủy khi khung giờ đã bắt đầu
    @Test
    void cancelAfterSlotStartedShouldBeRejected() {

        mockAuthenticatedUser();

        EventRegistration registration = createRegistration();
        registration.getSlot().setStartAt(
                LocalDateTime.now().minusMinutes(1)
        );

        when(registrationRepo.findById(10))
                .thenReturn(Optional.of(registration));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.cancelRegistration(10)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    // Test 11: Không được hủy đăng ký đã hủy
    @Test
    void cancelAlreadyCancelledRegistrationShouldBeRejected() {

        mockAuthenticatedUser();

        EventRegistration registration = createRegistration();
        registration.setRegistrationStatus(
                EventRegistrationStatus.CANCELLED
        );

        when(registrationRepo.findById(10))
                .thenReturn(Optional.of(registration));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.cancelRegistration(10)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void getMyRegistrationsWhenEmpty() {

        // 1. Giả lập USER đang đăng nhập
        mockAuthenticatedUser();

        // 2. Repository trả về danh sách rỗng
        when(registrationRepo
                .findByUser_UserIdOrderByRegisteredAtDesc(USER_ID))
                .thenReturn(List.of());

        // 3. Gọi Service
        var result = registrationService.getMyRegistrations();

        // 4. Kiểm tra
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMyRegistrationsSuccessfully() {

        // 1. Giả lập USER đang đăng nhập
        mockAuthenticatedUser();

        // 2. Đăng ký thứ nhất - mới nhất
        EventRegistration registration1 = createRegistration();
        registration1.setRegistrationId(11);
        registration1.setRegisteredAt(
                LocalDateTime.now().minusDays(1)
        );
        registration1.setRegistrationStatus(
                EventRegistrationStatus.REGISTERED
        );

        // 3. Đăng ký thứ hai - đã hủy
        EventRegistration registration2 = createRegistration();
        registration2.setRegistrationId(10);
        registration2.setRegisteredAt(
                LocalDateTime.now().minusDays(5)
        );
        registration2.setRegistrationStatus(
                EventRegistrationStatus.CANCELLED
        );

        // 4. Giả lập Repository trả về mới nhất trước
        when(registrationRepo
                .findByUser_UserIdOrderByRegisteredAtDesc(USER_ID))
                .thenReturn(
                        List.of(registration1, registration2)
                );

        // 5. Gọi Service
        var result = registrationService.getMyRegistrations();

        // 6. Kiểm tra số lượng
        assertEquals(2, result.size());

        // 7. Kiểm tra đăng ký mới nhất
        assertEquals(
                Integer.valueOf(11),
                result.get(0).registrationId()
        );

        assertEquals(
                EventRegistrationStatus.REGISTERED,
                result.get(0).registrationStatus()
        );

        // 8. Kiểm tra đăng ký đã hủy
        assertEquals(
                Integer.valueOf(10),
                result.get(1).registrationId()
        );

        assertEquals(
                EventRegistrationStatus.CANCELLED,
                result.get(1).registrationStatus()
        );

        // 9. Kiểm tra quyền sở hữu dữ liệu trả về
        assertEquals(USER_ID, result.get(0).userId());
        assertEquals(USER_ID, result.get(1).userId());
    }

}
