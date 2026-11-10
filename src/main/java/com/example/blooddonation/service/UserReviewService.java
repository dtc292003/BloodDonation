package com.example.blooddonation.service;

import com.example.blooddonation.entity.UserReview;
import com.example.blooddonation.repository.UserReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserReviewService {
    private final UserReviewRepository userReviewRepo;

    //Find All History
    public List<UserReview> getAllReviews(){
        return userReviewRepo.findAll();
    }

    //Find By Id
    public UserReview getByid(Integer id){
        return userReviewRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng ghi xét duyệt với ID: "+id));
    }

    // Find History By User
    public List<UserReview>getByUserId(Integer userId){
        return  userReviewRepo.findByUser_UserId(userId);
    }

    //Create UserReview
    public UserReview createReview(UserReview review){
        if(review.getReviewedAt() == null){
            review.setReviewedAt(LocalDateTime.now());
        }
        return userReviewRepo.save(review);
    }
}
