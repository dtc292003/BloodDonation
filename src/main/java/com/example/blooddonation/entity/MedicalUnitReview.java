
package com.example.blooddonation.entity;

import com.example.blooddonation.enums.MedicalUnitReviewDecision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medical_unit_review")
public class MedicalUnitReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_review_id")
    private Integer unitReviewId;

    // Đơn vị y tế được xét duyệt
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "medical_unit_id",
            nullable = false
    )
    private MedicalUnit medicalUnit;

    // ADMIN thực hiện xét duyệt
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reviewed_by_admin_id",
            nullable = false
    )
    private User reviewedByAdmin;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private MedicalUnitReviewDecision decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (reviewedAt == null) {
            reviewedAt = LocalDateTime.now();
        }
    }
}
