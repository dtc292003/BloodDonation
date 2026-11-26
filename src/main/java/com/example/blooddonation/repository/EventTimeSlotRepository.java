
package com.example.blooddonation.repository;

import com.example.blooddonation.entity.EventTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventTimeSlotRepository
        extends JpaRepository<EventTimeSlot, Integer> {

    // 1. Tìm khung giờ thuộc một sự kiện cụ thể
    Optional<EventTimeSlot> findBySlotIdAndEvent_EventId(
            Integer slotId,
            Integer eventId
    );

    // 2. Lấy danh sách khung giờ của sự kiện
    // Sắp xếp theo thời gian bắt đầu tăng dần
    List<EventTimeSlot> findByEvent_EventIdOrderByStartAtAsc(
            Integer eventId
    );
}
