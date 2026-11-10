package com.example.blooddonation.repository;

import com.example.blooddonation.entity.UserReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReviewRepository extends JpaRepository<UserReview, Integer> {

    // Find History By User
    List<UserReview> findByUser_UserId(Integer userId);
}
