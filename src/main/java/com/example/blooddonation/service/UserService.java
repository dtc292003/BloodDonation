
package com.example.blooddonation.service;

import com.example.blooddonation.dto.UserUpdateRequest;
import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.entity.UserReview;
import com.example.blooddonation.enums.ActionType;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.exception.DuplicateResourceException;
import com.example.blooddonation.exception.ResourceNotFoundException;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;
import com.example.blooddonation.dto.UserResponse;

import com.example.blooddonation.repository.UserReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class  UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserReviewRepository userReviewRepo;

    // Fill All
    public List<UserResponse> getAllUser() {
        return userRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Find By Id
    public User getById(Integer id) {

        return userRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy người dùng với ID: " + id
                        )
                );
    }

    // Change entity by dto
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getGender(),
                user.getAvatarUrl(),
                user.getEmailVerified(),
                user.getUserStatus(),
                user.getRole().getRoleName()
        );
    }
    public UserResponse getUserResponse(Integer id){
        User user = getById(id);
        return toResponse(user);
    }

    public UserResponse getUserResponseByEmail(String email){
        User user = getByEmail(email);
        return toResponse(user);
    }
    // CREATE USER
    public User createUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException(
                    "Email " + user.getEmail() + " đã được sử dụng"
            );
        }
        if (user.getRole() == null ||
                user.getRole().getRoleName() == null) {

            throw new RuntimeException(
                    "Vui lòng chọn loại tài khoản"
            );
        }
        Role role = roleRepo.findByRoleName(
                user.getRole().getRoleName()
        ).orElseThrow(() ->
                new RuntimeException("Không tìm thấy Role")
        );
        if (role.getRoleName() == RoleName.ADMIN) {

            throw new RuntimeException(
                    "Không được phép đăng ký tài khoản ADMIN"
            );
        }
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(false);
        user.setUserStatus(UserStatus.PENDING_EMAIL);
        return userRepo.save(user);
    }

    // UPDATE USER PROFILE
    public UserResponse updateUser(Integer id, UserUpdateRequest request) {
        User user = getById(id);

        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setDateOfBirth(request.dateOfBirth());
        user.setGender(request.gender());
        user.setAvatarUrl(request.avatarUrl());
        user.setAddress(request.address());
        user.setUpdatedAt(LocalDateTime.now());

        User updateUser=userRepo.save(user);

        return toResponse(updateUser);
    }

    // DELETE USER
    public void deleteUser(Integer id) {
        User user = getById(id);
        userRepo.delete(user);
    }

    // FIND BY EMAIL
    public User getByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy người dùng với email: " + email
                        )
                );
    }

    // Change Status
    public User changeStatusByAdmin(
            Integer targetUserId,
            Integer adminId,
            ActionType action,
            String reason
    ) {
        if (action == null) {
            throw new IllegalArgumentException(
                    "Hành động không được để trống"
            );
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập lý do thực hiện"
            );
        }
        User targetUser = getById(targetUserId);
        User admin = getById(adminId);
        if (admin.getRole() == null ||
                admin.getRole().getRoleName() != RoleName.ADMIN) {
            throw new IllegalArgumentException(
                    "Chỉ ADMIN mới được thực hiện thao tác này"
            );
        }
        if (admin.getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Tài khoản ADMIN không hoạt động"
            );
        }
        if (targetUser.getRole() != null &&
                targetUser.getRole().getRoleName() == RoleName.ADMIN) {
            throw new IllegalArgumentException(
                    "Không thể thay đổi trạng thái tài khoản ADMIN"
            );
        }
        UserStatus oldStatus = targetUser.getUserStatus();
        if (action == ActionType.APPROVE ||
                action == ActionType.REJECT) {
            if (targetUser.getRole() == null ||
                    targetUser.getRole().getRoleName() != RoleName.MEDICAL) {
                throw new IllegalArgumentException(
                        "Chỉ xét duyệt tài khoản MEDICAL"
                );
            }
            if (!Boolean.TRUE.equals(targetUser.getEmailVerified())) {
                throw new IllegalArgumentException(
                        "Tài khoản chưa xác minh email"
                );
            }
        }
        UserStatus newStatus;
        switch (action) {
            case APPROVE -> {
                if (oldStatus != UserStatus.PENDING_APPROVAL) {
                    throw new IllegalArgumentException(
                            "Tài khoản không ở trạng thái chờ duyệt"
                    );
                }
                newStatus = UserStatus.ACTIVE;
            }
            case REJECT -> {
                if (oldStatus != UserStatus.PENDING_APPROVAL) {
                    throw new IllegalArgumentException(
                            "Chỉ được từ chối tài khoản đang chờ duyệt"
                    );
                }
                newStatus = UserStatus.REJECTED;
            }
            case LOCK -> {
                if (oldStatus != UserStatus.ACTIVE) {
                    throw new IllegalArgumentException(
                            "Chỉ được khóa tài khoản đang hoạt động"
                    );
                }
                newStatus = UserStatus.LOCKED;
            }
            case UNLOCK -> {
                if (oldStatus != UserStatus.LOCKED) {
                    throw new IllegalArgumentException(
                            "Chỉ được mở khóa tài khoản đang bị khóa"
                    );
                }
                newStatus = UserStatus.ACTIVE;
            }

            default -> throw new IllegalArgumentException(
                    "Hành động không hợp lệ"
            );
        }
        LocalDateTime now = LocalDateTime.now();

        targetUser.setUserStatus(newStatus);
        targetUser.setUpdatedAt(now);

        User updatedUser = userRepo.save(targetUser);

        UserReview review = new UserReview();

        review.setUser(targetUser);
        review.setReviewedBy(admin);
        review.setActionType(action);
        review.setPreviousStatus(oldStatus.name());
        review.setNewStatus(newStatus.name());
        review.setReason(reason.trim());
        review.setReviewedAt(now);

        userReviewRepo.save(review);

        return updatedUser;
    }

}
