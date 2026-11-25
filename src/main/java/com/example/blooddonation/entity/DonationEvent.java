package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.EventApprovalStatus;
import com.example.blooddonation.enums.EventStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "donation_event",
       indexes = {@Index(name = "idx_donation_event_medical_unit_id", columnList = "medical_unit_id"), @Index(name = "idx_donation_event_location_id", columnList = "location_id"), @Index(name = "idx_donation_event_created_by_user_id", columnList = "created_by_user_id")})
public class DonationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_unit_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donation_event_medical_unit"))
    private MedicalUnit medicalUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donation_event_location"))
    private DonationLocation location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donation_event_created_by_user"))
    private User createdByUser;

    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "registration_open_at")
    private LocalDateTime registrationOpenAt;

    @Column(name = "registration_close_at")
    private LocalDateTime registrationCloseAt;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "approval_status", nullable = false, length = 20)
    private EventApprovalStatus approvalStatus = EventApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "event_status", nullable = false, length = 20)
    private EventStatus eventStatus = EventStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
