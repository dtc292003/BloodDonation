
package com.example.blooddonation.security;

import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.ApprovalStatus;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@RequiredArgsConstructor
public class ActiveUserJwtValidator
        implements OAuth2TokenValidator<Jwt> {

    private final UserRepository userRepo;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        // Chỉ chấp nhận Access Token
        if (!"access".equals(
                jwt.getClaimAsString("token_type")
        )) {
            return invalid();
        }

        // Lấy User ID từ JWT subject
        Integer userId;

        try {
            userId = Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            return invalid();
        }

        // Kiểm tra người dùng có tồn tại
        Optional<User> optionalUser =
                userRepo.findByIdWithRole(userId);

        if (optionalUser.isEmpty()) {
            return invalid();
        }

        User user = optionalUser.get();

        // Email phải được xác minh
        if (!Boolean.TRUE.equals(
                user.getEmailVerified()
        )) {
            return invalid();
        }

        // Tài khoản phải ACTIVE
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            return invalid();
        }

        // Kiểm tra Role hiện tại trong MySQL
        if (user.getRole() == null ||
                user.getRole().getRoleName() == null) {
            return invalid();
        }

        String currentRole =
                user.getRole().getRoleName().name();

        String tokenRole =
                jwt.getClaimAsString("role");

        if (!currentRole.equals(tokenRole)) {
            return invalid();
        }
        Object tokenVersionClaim =
                jwt.getClaim("token_version");

        if (!(tokenVersionClaim instanceof Number version)) {
            return invalid();
        }

        if (version.intValue() != user.getTokenVersion()) {
            return invalid();
        }

        if(user.getRole().getRoleName() == RoleName.MEDICAL) {
            if(user.getMedicalUnit() == null || user.getMedicalUnit().getApprovalStatus() != ApprovalStatus.APPROVED){
                return invalid();
            }
        }

        // JWT hợp lệ
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult invalid() {

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "Access Token khong con hop le",
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}
