package com.example.blooddonation.repository;

import com.example.blooddonation.entity.MedicalUnitReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalUnitReviewRepository extends JpaRepository<MedicalUnitReview, Integer> {
    List<MedicalUnitReview>  findByMedicalUnit_MedicalUnitIdOrderByReviewedAtDesc(
            Integer medicalUnitId
    );
}
