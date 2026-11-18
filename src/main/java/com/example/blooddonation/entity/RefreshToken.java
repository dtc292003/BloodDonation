package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "refresh_token")

public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Integer refreshTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String tokenHash;

    @Column(name = "created_at",nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at",nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private  Instant revokedAt;

    @PrePersist
    public void onCreate(){
        if(createdAt == null){
            createdAt = Instant.now();
        }
    }
}
