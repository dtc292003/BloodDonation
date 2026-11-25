package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.ABOGroup;
import com.example.blooddonation.enums.BloodTypeSource;
import com.example.blooddonation.enums.DonorAvailabilityStatus;
import com.example.blooddonation.enums.RhFactor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "donor_profile",
       uniqueConstraints = {@UniqueConstraint(name = "uk_donor_profile_user", columnNames = {"user_id"})})
public class DonorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donor_profile_id")
    private Integer donorProfileId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_donor_profile_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "abo_group", length = 2)
    private ABOGroup aboGroup;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rh_factor", nullable = false, length = 20)
    private RhFactor rhFactor = RhFactor.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "blood_type_source", nullable = false, length = 25)
    private BloodTypeSource bloodTypeSource = BloodTypeSource.UNKNOWN;

    @Column(name = "blood_type_updated_at")
    private LocalDateTime bloodTypeUpdatedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "availability_status", nullable = false, length = 20)
    private DonorAvailabilityStatus availabilityStatus = DonorAvailabilityStatus.UNAVAILABLE;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

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
