
package com.example.blooddonation.repository;

import com.example.blooddonation.entity.DonationRecord;
import com.example.blooddonation.enums.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRecordRepository
        extends JpaRepository<DonationRecord, Integer> {

    // Lấy lịch sử hiến máu theo người hiến và trạng thái,
    // sắp xếp từ lần hiến mới nhất đến cũ nhất.
    List<DonationRecord>
    findByDonorProfile_DonorProfileIdAndDonationStatusOrderByDonationDateDesc(
            Integer donorProfileId,
            DonationStatus donationStatus
    );
}
