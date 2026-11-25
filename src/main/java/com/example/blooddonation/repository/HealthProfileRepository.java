package com.example.blooddonation.repository;

import com.example.blooddonation.entity.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthProfileRepository extends JpaRepository<HealthProfile, Integer> {

    //Find HealthProfile By DonorProfileId
    Optional<HealthProfile>  findByDonorProfile_DonorProfileId(Integer donorProfileId);

    //Check HealthProfile
    boolean  existsByDonorProfile_DonorProfileId(Integer donorProfileId);
}
