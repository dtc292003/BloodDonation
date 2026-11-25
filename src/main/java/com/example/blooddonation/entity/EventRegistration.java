package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.CheckInStatus;
import com.example.blooddonation.enums.EventRegistrationStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "event_registration",
       uniqueConstraints = {@UniqueConstraint(name = "uk_event_registration_event_user", columnNames = {"event_id", "user_id"}), @UniqueConstraint(name = "uk_event_registration_qr_code_token", columnNames = {"qr_code_token"})},
       indexes = {@Index(name = "idx_event_registration_event_id", columnList = "event_id"), @Index(name = "idx_event_registration_slot_id", columnList = "slot_id"), @Index(name = "idx_event_registration_user_id", columnList = "user_id"), @Index(name = "idx_event_registration_checked_in_by_user_id", columnList = "checked_in_by_user_id")})
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Integer registrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_event_registration_event"))
    private DonationEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_event_registration_slot"))
    private EventTimeSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_event_registration_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "checked_in_by_user_id", nullable = true, unique = false,
                foreignKey = @ForeignKey(name = "fk_event_registration_checked_in_by_user"))
    private User checkedInByUser;

    @Column(name = "qr_code_token", nullable = false, length = 100)
    private String qrCodeToken;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "registration_status", nullable = false, length = 20)
    private EventRegistrationStatus registrationStatus = EventRegistrationStatus.REGISTERED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "check_in_status", nullable = false, length = 20)
    private CheckInStatus checkInStatus = CheckInStatus.NOT_CHECKED_IN;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (registeredAt == null) registeredAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
