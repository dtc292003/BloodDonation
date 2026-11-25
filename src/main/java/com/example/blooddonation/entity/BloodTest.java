package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.BloodTestConclusion;
import com.example.blooddonation.enums.BloodTestResult;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "blood_test",
       uniqueConstraints = {@UniqueConstraint(name = "uk_blood_test_donation", columnNames = {"donation_id"})})
public class BloodTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Integer testId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_blood_test_donation"))
    private DonationRecord donation;

    @Column(name = "hemoglobin", precision = 5, scale = 2)
    private BigDecimal hemoglobin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "hiv", nullable = false, length = 20)
    private BloodTestResult hiv = BloodTestResult.NOT_TESTED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "hepatitis_b", nullable = false, length = 20)
    private BloodTestResult hepatitisB = BloodTestResult.NOT_TESTED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "hepatitis_c", nullable = false, length = 20)
    private BloodTestResult hepatitisC = BloodTestResult.NOT_TESTED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "syphilis", nullable = false, length = 20)
    private BloodTestResult syphilis = BloodTestResult.NOT_TESTED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "conclusion", nullable = false, length = 20)
    private BloodTestConclusion conclusion = BloodTestConclusion.PENDING_REVIEW;

    @Column(name = "test_date", nullable = false)
    private LocalDateTime testDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

}
