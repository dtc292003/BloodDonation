package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.ContentReportStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "content_report",
       uniqueConstraints = {@UniqueConstraint(name = "uk_content_report_content_reported_by_user", columnNames = {"content_id", "reported_by_user_id"})},
       indexes = {@Index(name = "idx_content_report_content_id", columnList = "content_id"), @Index(name = "idx_content_report_reported_by_user_id", columnList = "reported_by_user_id"), @Index(name = "idx_content_report_handled_by_admin_id", columnList = "handled_by_admin_id")})
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_content_report_content"))
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_user_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_content_report_reported_by_user"))
    private User reportedByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "handled_by_admin_id", nullable = true, unique = false,
                foreignKey = @ForeignKey(name = "fk_content_report_handled_by_admin"))
    private User handledByAdmin;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "report_status", nullable = false, length = 20)
    private ContentReportStatus reportStatus = ContentReportStatus.PENDING;

    @Column(name = "handling_note", columnDefinition = "TEXT")
    private String handlingNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
    }

}
