package com.example.blooddonation.repository;

import com.example.blooddonation.entity.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonorProfileRepository extends JpaRepository<DonorProfile, Integer> {

    //Find DonorProfile By UserID
    Optional<DonorProfile> findByUser_UserId(Integer userId);

    //Check new DonorProfile
    boolean existsByUser_UserId(Integer userId);
}
