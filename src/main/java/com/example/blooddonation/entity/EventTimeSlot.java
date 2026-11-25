package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.SlotStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "event_time_slot",
       uniqueConstraints = {@UniqueConstraint(name = "uk_event_time_slot_event_slot", columnNames = {"event_id", "slot_id"})},
       indexes = {@Index(name = "idx_event_time_slot_event_id", columnList = "event_id")})
public class EventTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Integer slotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_event_time_slot_event"))
    private DonationEvent event;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "slot_status", nullable = false, length = 15)
    private SlotStatus slotStatus = SlotStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
    }

}
