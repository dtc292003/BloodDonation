
package com.example.blooddonation.repository;

import com.example.blooddonation.entity.MedicalVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalVerificationDocumentRepository
        extends JpaRepository<MedicalVerificationDocument, Integer> {

    Optional<MedicalVerificationDocument>
    findByMedicalUnit_MedicalUnitId(Integer medicalUnitId);
}
