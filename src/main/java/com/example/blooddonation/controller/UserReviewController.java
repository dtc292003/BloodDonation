package com.example.blooddonation.controller;

import com.example.blooddonation.entity.UserReview;
import com.example.blooddonation.service.UserReviewService;
import com.example.blooddonation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-review")
@RequiredArgsConstructor
public class UserReviewController {
    private final UserReviewService userReviewService;

    //Get All
    @GetMapping
    public ResponseEntity<List<UserReview>> getAll(){
        return ResponseEntity.ok(userReviewService.getAllReviews());
    }

    //GetById
    @GetMapping("/{id}")
    public ResponseEntity<UserReview> getById(@PathVariable Integer id){
        return ResponseEntity.ok(userReviewService.getByid(id));
    }

    //Get By UserId
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserReview>> getByUserId(@PathVariable Integer userId){
        return ResponseEntity.ok(userReviewService.getByUserId(userId));
    }
}
