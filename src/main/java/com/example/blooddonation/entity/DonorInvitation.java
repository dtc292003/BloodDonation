package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.ConnectionStatus;
import com.example.blooddonation.enums.InvitationSource;
import com.example.blooddonation.enums.InvitationStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "donor_invitation",
       uniqueConstraints = {@UniqueConstraint(name = "uk_donor_invitation_request_donor_profile", columnNames = {"request_id", "donor_profile_id"})},
       indexes = {@Index(name = "idx_donor_invitation_request_id", columnList = "request_id"), @Index(name = "idx_donor_invitation_donor_profile_id", columnList = "donor_profile_id")})
public class DonorInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Integer invitationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donor_invitation_request"))
    private BloodRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_profile_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_donor_invitation_donor_profile"))
    private DonorProfile donorProfile;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "invitation_source", nullable = false, length = 20)
    private InvitationSource invitationSource;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "invitation_status", nullable = false, length = 20)
    private InvitationStatus invitationStatus = InvitationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "connection_status", nullable = false, length = 20)
    private ConnectionStatus connectionStatus = ConnectionStatus.NOT_STARTED;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (sentAt == null) sentAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
