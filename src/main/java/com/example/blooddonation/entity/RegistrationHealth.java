package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "registration_health",
       uniqueConstraints = {@UniqueConstraint(name = "uk_registration_health_registration", columnNames = {"registration_id"})})
public class RegistrationHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_health_id")
    private Integer registrationHealthId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_registration_health_registration"))
    private EventRegistration registration;

    @Column(name = "abo_group_snapshot", length = 2)
    private String aboGroupSnapshot;

    @Column(name = "rh_factor_snapshot", length = 10)
    private String rhFactorSnapshot;

    @Column(name = "weight_kg_snapshot", precision = 5, scale = 2)
    private BigDecimal weightKgSnapshot;

    @Column(name = "medical_history_snapshot", columnDefinition = "TEXT")
    private String medicalHistorySnapshot;

    @Column(name = "allergies_snapshot", columnDefinition = "TEXT")
    private String allergiesSnapshot;

    @Column(name = "current_medication_snapshot", columnDefinition = "TEXT")
    private String currentMedicationSnapshot;

    @Column(name = "recent_health_status", columnDefinition = "TEXT")
    private String recentHealthStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_answers", columnDefinition = "JSON")
    private Map<String, Object> additionalAnswers;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

}
