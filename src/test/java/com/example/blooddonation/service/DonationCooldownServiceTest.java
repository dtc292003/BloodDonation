
package com.example.blooddonation.service;

import com.example.blooddonation.entity.DonationRecord;
import com.example.blooddonation.enums.DonationStatus;
import com.example.blooddonation.enums.DonationType;
import com.example.blooddonation.repository.DonationRecordRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationCooldownServiceTest {

    @Mock
    private DonationRecordRepository donationRecordRepo;

    @InjectMocks
    private DonationCooldownService cooldownService;

    private static final Integer DONOR_ID = 1;

    // Tạo dữ liệu kiểm thử
    private DonationRecord createRecord(
            DonationType type,
            long daysAgo
    ) {
        DonationRecord record = new DonationRecord();

        record.setDonationType(type);
        record.setDonationDate(
                LocalDateTime.now().minusDays(daysAgo)
        );
        record.setDonationStatus(DonationStatus.COMPLETED);

        return record;
    }

    private void mockHistory(List<DonationRecord> records) {
        when(donationRecordRepo
                .findByDonorProfile_DonorProfileIdAndDonationStatusOrderByDonationDateDesc(
                        DONOR_ID,
                        DonationStatus.COMPLETED
                )
        ).thenReturn(records);
    }

    // Test 1: Chưa có lịch sử hiến máu
    @Test
    void noDonationHistory() {
        mockHistory(List.of());

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertTrue(result.meetsMinimumInterval());
        assertFalse(result.requiresMedicalReview());
        assertNull(result.nextAllowedAt());
    }

    // Test 2: Hiến máu toàn phần 30 ngày trước
    @Test
    void wholeBloodWithin84Days() {
        mockHistory(List.of(
                createRecord(DonationType.WHOLE_BLOOD, 30)
        ));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertFalse(result.meetsMinimumInterval());
        assertFalse(result.requiresMedicalReview());
        assertNotNull(result.nextAllowedAt());
    }

    // Test 3: Hiến máu toàn phần 90 ngày trước
    @Test
    void wholeBloodAfter84Days() {
        mockHistory(List.of(
                createRecord(DonationType.WHOLE_BLOOD, 90)
        ));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertTrue(result.meetsMinimumInterval());
    }

    // Test 4: Hiến tiểu cầu 10 ngày trước
    @Test
    void plateletsWithin14Days() {
        mockHistory(List.of(
                createRecord(DonationType.PLATELETS, 10)
        ));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertFalse(result.meetsMinimumInterval());
    }

    // Test 5: Hiến huyết tương 15 ngày trước
    @Test
    void plasmaAfter14Days() {
        mockHistory(List.of(
                createRecord(DonationType.PLASMA, 15)
        ));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertTrue(result.meetsMinimumInterval());
    }

    // Test 6: Loại hiến OTHER
    @Test
    void otherDonationRequiresMedicalReview() {
        mockHistory(List.of(
                createRecord(DonationType.OTHER, 30)
        ));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertFalse(result.meetsMinimumInterval());
        assertTrue(result.requiresMedicalReview());
        assertNull(result.nextAllowedAt());
    }

    // Test 7: ID không hợp lệ
    @Test
    void invalidDonorId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> cooldownService.checkCooldown(null)
        );
    }

    // Test 8: Máu toàn phần chưa đủ 84 ngày (còn 1 phút)
    @Test
    void wholeBloodJustBefore84Days() {
        DonationRecord record = createRecord(
                DonationType.WHOLE_BLOOD, 84
        );

        record.setDonationDate(
                LocalDateTime.now()
                        .minusDays(84)
                        .plusMinutes(1)
        );

        mockHistory(List.of(record));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertFalse(result.meetsMinimumInterval());
    }

    // Test 9: Tiểu cầu đã đủ 14 ngày (quá 1 phút)
    @Test
    void plateletsJustAfter14Days() {
        DonationRecord record = createRecord(
                DonationType.PLATELETS, 14
        );

        record.setDonationDate(
                LocalDateTime.now()
                        .minusDays(14)
                        .minusMinutes(1)
        );

        mockHistory(List.of(record));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertTrue(result.meetsMinimumInterval());
    }

    // Test 10: Ngày hiến nằm trong tương lai
    @Test
    void futureDonationDateRequiresReview() {
        DonationRecord record = createRecord(
                DonationType.WHOLE_BLOOD, 0
        );

        record.setDonationDate(
                LocalDateTime.now().plusDays(1)
        );

        mockHistory(List.of(record));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertFalse(result.meetsMinimumInterval());
        assertTrue(result.requiresMedicalReview());
    }


    // Test 11: Hiến máu toàn phần, sau đó hiến tiểu cầu
    @Test
    void mixedDonationLatestPlatelets() {

        DonationRecord platelets = createRecord(
                DonationType.PLATELETS, 10
        );

        DonationRecord wholeBlood = createRecord(
                DonationType.WHOLE_BLOOD, 100
        );

        // Lịch sử được sắp xếp mới nhất trước
        mockHistory(List.of(platelets, wholeBlood));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertEquals(
                DonationType.PLATELETS,
                result.lastDonationType()
        );

        assertFalse(result.meetsMinimumInterval());
        assertFalse(result.requiresMedicalReview());
    }

    // Test 12: Hiến tiểu cầu, sau đó hiến máu toàn phần
    @Test
    void mixedDonationLatestWholeBlood() {

        DonationRecord wholeBlood = createRecord(
                DonationType.WHOLE_BLOOD, 40
        );

        DonationRecord platelets = createRecord(
                DonationType.PLATELETS, 100
        );

        mockHistory(List.of(wholeBlood, platelets));

        var result = cooldownService.checkCooldown(DONOR_ID);

        assertEquals(
                DonationType.WHOLE_BLOOD,
                result.lastDonationType()
        );

        assertFalse(result.meetsMinimumInterval());
        assertFalse(result.requiresMedicalReview());
    }


}
