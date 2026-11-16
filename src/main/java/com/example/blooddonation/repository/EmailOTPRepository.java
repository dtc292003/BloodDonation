package com.example.blooddonation.repository;

import com.example.blooddonation.entity.EmailOTP;
import com.example.blooddonation.enums.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOTPRepository extends JpaRepository<EmailOTP, Integer> {

    //Find first OTP
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailOTP>
    findFirstByUser_UserIdAndPurposeOrderByCreatedAtDescOtpIdDesc(
            Integer userId,
            OtpPurpose purpose
    );
}


