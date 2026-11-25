
package com.example.blooddonation.service;

import com.example.blooddonation.entity.DonationRecord;
import com.example.blooddonation.enums.DonationStatus;
import com.example.blooddonation.enums.DonationType;
import com.example.blooddonation.repository.DonationRecordRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationCooldownService {

    private final DonationRecordRepository donationRecordRepo;

    private static final long WHOLE_BLOOD_DAYS = 84;
    private static final long COMPONENT_DAYS = 14;

    // Kiểm tra khoảng nghỉ sau lần hiến hoàn thành gần nhất
    @Transactional(readOnly = true)
    public CooldownResult checkCooldown(Integer donorProfileId) {

        if (donorProfileId == null || donorProfileId <= 0) {
            throw new IllegalArgumentException(
                    "DonorProfileID không hợp lệ"
            );
        }

        List<DonationRecord> records = donationRecordRepo
                .findByDonorProfile_DonorProfileIdAndDonationStatusOrderByDonationDateDesc(
                        donorProfileId,
                        DonationStatus.COMPLETED
                );

        // Chưa ghi nhận lần hiến hoàn thành nào
        if (records.isEmpty()) {
            return new CooldownResult(
                    true,
                    false,
                    null,
                    null,
                    null,
                    "Chưa có lịch sử hiến máu hoàn thành"
            );
        }

        DonationRecord latest = records.get(0);

        LocalDateTime lastDonationAt = latest.getDonationDate();
        DonationType lastDonationType = latest.getDonationType();
        LocalDateTime now = LocalDateTime.now();

        // Dữ liệu không đủ tin cậy để tự động tính
        if (lastDonationAt == null
                || lastDonationAt.isAfter(now)
                || lastDonationType == null
                || lastDonationType == DonationType.OTHER) {

            return new CooldownResult(
                    false,
                    true,
                    lastDonationAt,
                    lastDonationType,
                    null,
                    "Cần nhân viên y tế kiểm tra lịch sử hiến máu"
            );
        }

        // Xác định khoảng nghỉ theo loại hiến gần nhất
        long minimumDays = switch (lastDonationType) {
            case WHOLE_BLOOD -> WHOLE_BLOOD_DAYS;
            case PLATELETS, PLASMA -> COMPONENT_DAYS;
            case OTHER -> throw new IllegalStateException(
                    "Loại hiến chưa được hỗ trợ"
            );
        };

        // Thời điểm kết thúc khoảng nghỉ
        LocalDateTime nextAllowedAt =
                lastDonationAt.plusDays(minimumDays);

        // Đã đủ thời gian nghỉ hay chưa?
        boolean meetsInterval =
                !now.isBefore(nextAllowedAt);

        String message = meetsInterval
                ? "Đã đủ khoảng nghỉ tối thiểu giữa các lần hiến"
                : "Chưa đủ khoảng nghỉ tối thiểu giữa các lần hiến";

        return new CooldownResult(
                meetsInterval,
                false,
                lastDonationAt,
                lastDonationType,
                nextAllowedAt,
                message
        );
    }

    // Kết quả kiểm tra khoảng nghỉ
    public record CooldownResult(

            boolean meetsMinimumInterval,

            boolean requiresMedicalReview,

            LocalDateTime lastDonationAt,

            DonationType lastDonationType,

            LocalDateTime nextAllowedAt,

            String message

    ) {}
}
