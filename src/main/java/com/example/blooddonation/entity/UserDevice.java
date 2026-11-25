package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.blooddonation.enums.DevicePlatform;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_device",
       uniqueConstraints = {@UniqueConstraint(name = "uk_user_device_device_token", columnNames = {"device_token"})},
       indexes = {@Index(name = "idx_user_device_user_id", columnList = "user_id")})
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Integer deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = false,
                foreignKey = @ForeignKey(name = "fk_user_device_user"))
    private User user;

    @Column(name = "device_token", nullable = false, length = 512)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "platform", nullable = false, length = 15)
    private DevicePlatform platform;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
    }

}
