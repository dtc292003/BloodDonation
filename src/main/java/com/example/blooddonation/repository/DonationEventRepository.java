
package com.example.blooddonation.repository;

import com.example.blooddonation.entity.DonationEvent;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonationEventRepository
        extends JpaRepository<DonationEvent, Integer> {

    // Khóa sự kiện khi xử lý đăng ký để tránh vượt sức chứa
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM DonationEvent e WHERE e.eventId = :eventId")
    Optional<DonationEvent> findByIdForUpdate(
            @Param("eventId") Integer eventId
    );
}
