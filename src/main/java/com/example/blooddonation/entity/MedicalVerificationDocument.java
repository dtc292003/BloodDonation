
package com.example.blooddonation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_verification_document")
@Getter
@Setter
@NoArgsConstructor
public class MedicalVerificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "medical_unit_id",
            nullable = false,
            unique = true
    )
    private MedicalUnit medicalUnit;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Lob
    @Column(
            name = "file_data",
            nullable = false,
            columnDefinition = "LONGBLOB"
    )
    private byte[] fileData;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
