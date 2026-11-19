package com.example.blooddonation.repository;

import com.example.blooddonation.entity.MedicalUnit;
import com.example.blooddonation.entity.MedicalUnitReview;
import com.example.blooddonation.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalUnitRepository extends JpaRepository<MedicalUnit, Integer> {
    List<MedicalUnit> findByApprovalStatus(
            ApprovalStatus status
    );
}
