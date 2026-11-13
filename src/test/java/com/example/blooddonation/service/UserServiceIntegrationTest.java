
package com.example.blooddonation.service;

import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.entity.UserReview;
import com.example.blooddonation.enums.*;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistUserStatusAndReviewWhenAdminLocksUser() {

        // 1. Lấy ROLE có sẵn trong MySQL
        Role adminRole = roleRepo.findByRoleName(RoleName.ADMIN)
                .orElseThrow();

        Role userRole = roleRepo.findByRoleName(RoleName.USER)
                .orElseThrow();

        // 2. Tạo dữ liệu kiểm thử
        User admin = createTestUser(
                adminRole, "Admin Test", "0980000001"
        );

        User targetUser = createTestUser(
                userRole, "User Test", "0980000002"
        );

        // 3. Thực hiện LOCK bằng Service thật
        userService.changeStatusByAdmin(
                targetUser.getUserId(),
                admin.getUserId(),
                ActionType.LOCK,
                "Kiểm thử Integration Test"
        );

        // 4. Ghi SQL xuống database và xóa cache JPA
        entityManager.flush();
        entityManager.clear();

        // 5. Đọc lại trạng thái USER từ database
        User updatedUser = userRepo.findById(
                targetUser.getUserId()
        ).orElseThrow();

        assertEquals(
                UserStatus.LOCKED,
                updatedUser.getUserStatus()
        );

        // 6. Truy vấn lịch sử USER_REVIEW
        List<UserReview> reviews = entityManager.createQuery(
                        "SELECT r FROM UserReview r " +
                                "WHERE r.user.userId = :userId",
                        UserReview.class
                )
                .setParameter("userId", targetUser.getUserId())
                .getResultList();

        // 7. Kiểm tra số lượng lịch sử
        assertEquals(1, reviews.size());

        UserReview review = reviews.get(0);

        // 8. Kiểm tra dữ liệu lịch sử
        assertEquals(ActionType.LOCK, review.getActionType());
        assertEquals("ACTIVE", review.getPreviousStatus());
        assertEquals("LOCKED", review.getNewStatus());

        assertEquals(
                "Kiểm thử Integration Test",
                review.getReason()
        );

        assertEquals(
                admin.getUserId(),
                review.getReviewedBy().getUserId()
        );

        assertNotNull(review.getReviewedAt());
    }

    // Phương thức tạo tài khoản kiểm thử
    private User createTestUser(
            Role role,
            String fullName,
            String phoneNumber
    ) {
        User user = new User();

        user.setFullName(fullName);
        user.setEmail(
                "integration-" + UUID.randomUUID() + "@example.com"
        );
        user.setPasswordHash("test-only-hash");
        user.setPhoneNumber(phoneNumber);
        user.setDateOfBirth(LocalDate.of(2000, 1, 1));
        user.setGender(Gender.NAM);
        user.setEmailVerified(true);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.saveAndFlush(user);
    }
}
