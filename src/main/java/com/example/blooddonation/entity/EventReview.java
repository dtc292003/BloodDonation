package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.EventReviewDecision;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "event_review",
       indexes = {@Index(name = "idx_event_review_event_id", columnList = "event_id"), @Index(name = "idx_event_review_reviewed_by_admin_id", columnList = "reviewed_by_admin_id")})
public class EventReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_review_id")
    private Integer eventReviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_event_review_event"))
    private DonationEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewed_by_admin_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_event_review_reviewed_by_admin"))
    private User reviewedByAdmin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "decision", nullable = false, length = 15)
    private EventReviewDecision decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (reviewedAt == null) reviewedAt = now;
    }

}
