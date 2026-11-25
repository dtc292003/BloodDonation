package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.DonationStatus;
import com.example.blooddonation.enums.DonationType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "donation_record",
       uniqueConstraints = {@UniqueConstraint(name = "uk_donation_record_registration", columnNames = {"registration_id"}), @UniqueConstraint(name = "uk_donation_record_invitation", columnNames = {"invitation_id"})},
       indexes = {@Index(name = "idx_donation_record_donor_profile_id", columnList = "donor_profile_id"), @Index(name = "idx_donation_record_medical_unit_id", columnList = "medical_unit_id"), @Index(name = "idx_donation_record_confirmed_by_user_id", columnList = "confirmed_by_user_id")})
public class DonationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donation_id")
    private Integer donationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_profile_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donation_record_donor_profile"))
    private DonorProfile donorProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_unit_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donation_record_medical_unit"))
    private MedicalUnit medicalUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmed_by_user_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donation_record_confirmed_by_user"))
    private User confirmedByUser;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "registration_id", nullable = true, unique = true,
                foreignKey = @ForeignKey(name = "fk_donation_record_registration"))
    private EventRegistration registration;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "invitation_id", nullable = true, unique = true,
                foreignKey = @ForeignKey(name = "fk_donation_record_invitation"))
    private DonorInvitation invitation;

    @Column(name = "donation_date", nullable = false)
    private LocalDateTime donationDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "donation_type", nullable = false, length = 20)
    private DonationType donationType;

    @Column(name = "volume_ml")
    private Integer volumeMl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "donation_status", nullable = false, length = 25)
    private DonationStatus donationStatus = DonationStatus.PENDING_CONFIRMATION;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

}
