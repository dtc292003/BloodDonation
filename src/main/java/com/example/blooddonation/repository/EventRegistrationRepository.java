package com.example.blooddonation.repository;

import com.example.blooddonation.entity.EventRegistration;
import com.example.blooddonation.enums.EventRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Integer> {
    // Tìm đăng ký User trong sự kiện
    Optional<EventRegistration> findByEvent_EventIdAndUser_UserId(
            Integer eventId,
            Integer userId
    );

    //Đếm số người đăng ký trong sự kiện
    long countByEvent_EventIdAndRegistrationStatus(
            Integer eventId,
            EventRegistrationStatus status
    );

    //Đếm số người đăng ký trong khung giờ
    long countBySlot_SlotIdAndRegistrationStatus(
            Integer slotId,
            EventRegistrationStatus status
    );

    //Lấy lịch sử đăng ký sự kiện của User
    List<EventRegistration> findByUser_UserIdOrderByRegisteredAtDesc(
            Integer userId
    );
}
