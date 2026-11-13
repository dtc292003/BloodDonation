
package com.example.blooddonation.service;

import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.entity.UserReview;
import com.example.blooddonation.enums.*;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;
import com.example.blooddonation.repository.UserReviewRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class UserServiceRollbackIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @PersistenceContext
    private EntityManager entityManager;

    // Thay Repository này bằng Mockito trong bài test
    @MockitoBean
    private UserReviewRepository userReviewRepo;

    private Integer adminId;
    private Integer targetUserId;

    @Test
    void shouldRollbackUserStatusWhenReviewSaveFails() {

        // 1. Lấy ROLE có sẵn
        Role adminRole = roleRepo.findByRoleName(RoleName.ADMIN)
                .orElseThrow();

        Role userRole = roleRepo.findByRoleName(RoleName.USER)
                .orElseThrow();

        // 2. Tạo hai tài khoản trong MySQL
        User admin = createTestUser(adminRole, "Admin Rollback Test");
        adminId = admin.getUserId();

        User targetUser = createTestUser(userRole, "User Rollback Test");
        targetUserId = targetUser.getUserId();

        // 3. Giả lập lỗi khi lưu UserReview
        doAnswer(invocation -> {

            // Đẩy lệnh UPDATE USER xuống MySQL
            entityManager.flush();

            throw new RuntimeException(
                    "Giả lập lỗi lưu USER_REVIEW"
            );

        }).when(userReviewRepo).save(any(UserReview.class));

        // 4. Gọi Service và xác nhận có Exception
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.changeStatusByAdmin(
                        targetUserId,
                        adminId,
                        ActionType.LOCK,
                        "Kiểm thử rollback"
                )
        );

        assertEquals(
                "Giả lập lỗi lưu USER_REVIEW",
                exception.getMessage()
        );

        // 5. Đọc lại USER từ MySQL sau khi transaction rollback
        User reloadedUser = userRepo.findById(targetUserId)
                .orElseThrow();

        // 6. Trạng thái phải quay về ACTIVE
        assertEquals(
                UserStatus.ACTIVE,
                reloadedUser.getUserStatus()
        );

        // 7. Kiểm tra đã thực hiện thao tác lưu review
        verify(userReviewRepo, times(1))
                .save(any(UserReview.class));

        System.out.println(
                "USER status sau rollback: "
                        + reloadedUser.getUserStatus()
        );
    }

    // Tạo dữ liệu kiểm thử
    private User createTestUser(Role role, String fullName) {

        User user = new User();

        user.setFullName(fullName);
        user.setEmail(
                "rollback-" + UUID.randomUUID() + "@example.com"
        );
        user.setPasswordHash("test-only-hash");
        user.setPhoneNumber("0980000000");
        user.setDateOfBirth(LocalDate.of(2000, 1, 1));
        user.setGender(Gender.NAM);
        user.setEmailVerified(true);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.saveAndFlush(user);
    }

    // Xóa dữ liệu thử nghiệm sau khi kiểm tra
    @AfterEach
    void cleanup() {

        if (targetUserId != null
                && userRepo.existsById(targetUserId)) {
            userRepo.deleteById(targetUserId);
        }

        if (adminId != null
                && userRepo.existsById(adminId)) {
            userRepo.deleteById(adminId);
        }
    }
}
