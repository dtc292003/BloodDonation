
package com.example.blooddonation.service;

import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.entity.UserReview;
import com.example.blooddonation.enums.ActionType;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;
import com.example.blooddonation.repository.UserReviewRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private UserReviewRepository userReviewRepo;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldLockActiveUserAndCreateReview() {

        // 1. Giả lập ADMIN
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        User admin = new User();
        admin.setUserId(1);
        admin.setRole(adminRole);
        admin.setUserStatus(UserStatus.ACTIVE);

        // 2. Giả lập USER cần khóa
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        User targetUser = new User();
        targetUser.setUserId(2);
        targetUser.setRole(userRole);
        targetUser.setUserStatus(UserStatus.ACTIVE);

        // 3. Mock dữ liệu Repository
        when(userRepo.findById(1))
                .thenReturn(Optional.of(admin));

        when(userRepo.findById(2))
                .thenReturn(Optional.of(targetUser));

        when(userRepo.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 4. Gọi phương thức cần kiểm thử
        User result = userService.changeStatusByAdmin(
                2,
                1,
                ActionType.LOCK,
                "Vi phạm quy định"
        );

        // 5. Kiểm tra trạng thái USER
        assertEquals(UserStatus.LOCKED, result.getUserStatus());

        // 6. Kiểm tra lịch sử xử lý được lưu
        // Bắt đối tượng UserReview được truyền vào save()
        ArgumentCaptor<UserReview> captor =
                ArgumentCaptor.forClass(UserReview.class);

        verify(userReviewRepo, times(1))
                .save(captor.capture());

// Lấy bản ghi UserReview đã được tạo
        UserReview review = captor.getValue();

// Kiểm tra nội dung
        assertEquals(ActionType.LOCK, review.getActionType());
        assertEquals("ACTIVE", review.getPreviousStatus());
        assertEquals("LOCKED", review.getNewStatus());
        assertEquals("Vi phạm quy định", review.getReason());

        assertEquals(2, review.getUser().getUserId());
        assertEquals(1, review.getReviewedBy().getUserId());
    }
    @Test
    void shouldUnlockLockedUserAndCreateReview() {

        // 1. Tạo ADMIN
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        User admin = new User();
        admin.setUserId(1);
        admin.setRole(adminRole);
        admin.setUserStatus(UserStatus.ACTIVE);

        // 2. Tạo USER đang bị khóa
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        User targetUser = new User();
        targetUser.setUserId(2);
        targetUser.setRole(userRole);
        targetUser.setUserStatus(UserStatus.LOCKED);

        // 3. Giả lập Repository
        when(userRepo.findById(1))
                .thenReturn(Optional.of(admin));

        when(userRepo.findById(2))
                .thenReturn(Optional.of(targetUser));

        when(userRepo.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 4. Gọi Service
        User result = userService.changeStatusByAdmin(
                2,
                1,
                ActionType.UNLOCK,
                "Đã xác minh và cho phép hoạt động lại"
        );

        // 5. Kiểm tra trạng thái USER
        assertEquals(UserStatus.ACTIVE, result.getUserStatus());

        // 6. Kiểm tra lịch sử USER_REVIEW
        ArgumentCaptor<UserReview> captor =
                ArgumentCaptor.forClass(UserReview.class);

        verify(userReviewRepo, times(1))
                .save(captor.capture());

        UserReview review = captor.getValue();

        assertEquals(ActionType.UNLOCK, review.getActionType());
        assertEquals("LOCKED", review.getPreviousStatus());
        assertEquals("ACTIVE", review.getNewStatus());
        assertEquals(2, review.getUser().getUserId());
        assertEquals(1, review.getReviewedBy().getUserId());
        assertEquals(
                "Đã xác minh và cho phép hoạt động lại",
                review.getReason()
        );
    }

    @Test
    void shouldApprovePendingMedicalAndCreateReview() {

        // 1. Tạo Role ADMIN
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        // 2. Tạo tài khoản ADMIN đang hoạt động
        User admin = new User();
        admin.setUserId(1);
        admin.setRole(adminRole);
        admin.setUserStatus(UserStatus.ACTIVE);

        // 3. Tạo Role MEDICAL
        Role medicalRole = new Role();
        medicalRole.setRoleName(RoleName.MEDICAL);

        // 4. Tạo tài khoản MEDICAL đang chờ duyệt
        User targetUser = new User();
        targetUser.setUserId(3);
        targetUser.setRole(medicalRole);
        targetUser.setUserStatus(UserStatus.PENDING_APPROVAL);
        targetUser.setEmailVerified(true);

        // 5. Giả lập Repository tìm tài khoản
        when(userRepo.findById(1))
                .thenReturn(Optional.of(admin));

        when(userRepo.findById(3))
                .thenReturn(Optional.of(targetUser));

        // 6. Giả lập lưu User
        when(userRepo.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 7. Gọi phương thức cần kiểm thử
        User result = userService.changeStatusByAdmin(
                3,
                1,
                ActionType.APPROVE,
                "Hồ sơ đơn vị y tế hợp lệ"
        );

        // 8. Kiểm tra trạng thái sau khi duyệt
        assertEquals(UserStatus.ACTIVE, result.getUserStatus());

        // 9. Bắt đối tượng UserReview được lưu
        ArgumentCaptor<UserReview> captor =
                ArgumentCaptor.forClass(UserReview.class);

        verify(userReviewRepo, times(1))
                .save(captor.capture());

        UserReview review = captor.getValue();

        // 10. Kiểm tra nội dung lịch sử
        assertEquals(ActionType.APPROVE, review.getActionType());

        assertEquals(
                "PENDING_APPROVAL",
                review.getPreviousStatus()
        );

        assertEquals("ACTIVE", review.getNewStatus());

        assertEquals(
                "Hồ sơ đơn vị y tế hợp lệ",
                review.getReason()
        );

        // 11. Kiểm tra tài khoản được duyệt
        assertEquals(3, review.getUser().getUserId());

        // 12. Kiểm tra ADMIN thực hiện xét duyệt
        assertEquals(1, review.getReviewedBy().getUserId());

        // 13. Kiểm tra thời gian xét duyệt
        assertNotNull(review.getReviewedAt());

        // 14. Xác nhận User được lưu đúng một lần
        verify(userRepo, times(1))
                .save(targetUser);
    }

    @Test
    void shouldRejectPendingMedicalAndCreateReview() {

        // 1. Tạo tài khoản ADMIN
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        User admin = new User();
        admin.setUserId(1);
        admin.setRole(adminRole);
        admin.setUserStatus(UserStatus.ACTIVE);

        // 2. Tạo tài khoản MEDICAL chờ duyệt
        Role medicalRole = new Role();
        medicalRole.setRoleName(RoleName.MEDICAL);

        User targetUser = new User();
        targetUser.setUserId(3);
        targetUser.setRole(medicalRole);
        targetUser.setUserStatus(UserStatus.PENDING_APPROVAL);
        targetUser.setEmailVerified(true);

        // 3. Giả lập Repository
        when(userRepo.findById(1))
                .thenReturn(Optional.of(admin));

        when(userRepo.findById(3))
                .thenReturn(Optional.of(targetUser));

        when(userRepo.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 4. Gọi Service
        User result = userService.changeStatusByAdmin(
                3,
                1,
                ActionType.REJECT,
                "Thông tin đơn vị y tế không hợp lệ"
        );

        // 5. Kiểm tra trạng thái USER
        assertEquals(UserStatus.REJECTED, result.getUserStatus());

        // 6. Kiểm tra lịch sử USER_REVIEW
        ArgumentCaptor<UserReview> captor =
                ArgumentCaptor.forClass(UserReview.class);

        verify(userReviewRepo, times(1))
                .save(captor.capture());

        UserReview review = captor.getValue();

        assertEquals(ActionType.REJECT, review.getActionType());
        assertEquals("PENDING_APPROVAL", review.getPreviousStatus());
        assertEquals("REJECTED", review.getNewStatus());

        assertEquals(
                "Thông tin đơn vị y tế không hợp lệ",
                review.getReason()
        );

        assertEquals(3, review.getUser().getUserId());
        assertEquals(1, review.getReviewedBy().getUserId());
        assertNotNull(review.getReviewedAt());

        verify(userRepo, times(1)).save(targetUser);
    }

    @Test
    void shouldNotLockAlreadyLockedUser() {

        // 1. Tạo ADMIN đang hoạt động
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        User admin = new User();
        admin.setUserId(1);
        admin.setRole(adminRole);
        admin.setUserStatus(UserStatus.ACTIVE);

        // 2. Tạo USER đã bị khóa
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        User targetUser = new User();
        targetUser.setUserId(2);
        targetUser.setRole(userRole);
        targetUser.setUserStatus(UserStatus.LOCKED);

        // 3. Giả lập Repository
        when(userRepo.findById(1))
                .thenReturn(Optional.of(admin));

        when(userRepo.findById(2))
                .thenReturn(Optional.of(targetUser));

        // 4. Kiểm tra Service phải phát sinh lỗi
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changeStatusByAdmin(
                        2,
                        1,
                        ActionType.LOCK,
                        "Khóa tài khoản lần nữa"
                )
        );

        // 5. Kiểm tra thông báo lỗi
        assertEquals(
                "Chỉ được khóa tài khoản đang hoạt động",
                exception.getMessage()
        );

        // 6. Trạng thái USER không được thay đổi
        assertEquals(UserStatus.LOCKED, targetUser.getUserStatus());

        // 7. Không được lưu USER hoặc USER_REVIEW
        verify(userRepo, never()).save(any(User.class));
        verify(userReviewRepo, never()).save(any(UserReview.class));
    }

    @Test
    void shouldRejectActionWhenActorIsNotAdmin() {

        // 1. Tạo tài khoản USER thực hiện thao tác
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        User actor = new User();
        actor.setUserId(1);
        actor.setRole(userRole);
        actor.setUserStatus(UserStatus.ACTIVE);

        // 2. Tạo tài khoản mục tiêu
        User targetUser = new User();
        targetUser.setUserId(2);
        targetUser.setRole(userRole);
        targetUser.setUserStatus(UserStatus.ACTIVE);

        // 3. Giả lập Repository
        when(userRepo.findById(1))
                .thenReturn(Optional.of(actor));

        when(userRepo.findById(2))
                .thenReturn(Optional.of(targetUser));

        // 4. Gọi Service và kiểm tra lỗi
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changeStatusByAdmin(
                        2,
                        1,
                        ActionType.LOCK,
                        "Khóa tài khoản"
                )
        );

        // 5. Kiểm tra thông báo
        assertEquals(
                "Chỉ ADMIN mới được thực hiện thao tác này",
                exception.getMessage()
        );

        // 6. Trạng thái mục tiêu không thay đổi
        assertEquals(
                UserStatus.ACTIVE,
                targetUser.getUserStatus()
        );

        // 7. Không được lưu USER và USER_REVIEW
        verify(userRepo, never()).save(any(User.class));
        verify(userReviewRepo, never())
                .save(any(UserReview.class));
    }

    @Test
    void shouldRejectApprovalWhenMedicalEmailNotVerified() {

        // 1. Tạo ADMIN
        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        User admin = new User();
        admin.setUserId(1);
        admin.setRole(adminRole);
        admin.setUserStatus(UserStatus.ACTIVE);

        // 2. Tạo MEDICAL chưa xác minh email
        Role medicalRole = new Role();
        medicalRole.setRoleName(RoleName.MEDICAL);

        User targetUser = new User();
        targetUser.setUserId(3);
        targetUser.setRole(medicalRole);
        targetUser.setUserStatus(UserStatus.PENDING_APPROVAL);
        targetUser.setEmailVerified(false);

        // 3. Giả lập Repository
        when(userRepo.findById(1))
                .thenReturn(Optional.of(admin));

        when(userRepo.findById(3))
                .thenReturn(Optional.of(targetUser));

        // 4. Thực hiện APPROVE và kiểm tra lỗi
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changeStatusByAdmin(
                        3,
                        1,
                        ActionType.APPROVE,
                        "Hồ sơ hợp lệ"
                )
        );

        // 5. Kiểm tra thông báo
        assertEquals(
                "Tài khoản chưa xác minh email",
                exception.getMessage()
        );

        // 6. Trạng thái MEDICAL không thay đổi
        assertEquals(
                UserStatus.PENDING_APPROVAL,
                targetUser.getUserStatus()
        );

        // 7. Không được lưu User hoặc UserReview
        verify(userRepo, never()).save(any(User.class));

        verify(userReviewRepo, never())
                .save(any(UserReview.class));
    }

    @Test
    void shouldRejectActionWhenReasonIsBlank() {

        // 1. Gọi Service với lý do chỉ chứa khoảng trắng
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changeStatusByAdmin(
                        2,
                        1,
                        ActionType.LOCK,
                        "   "
                )
        );

        // 2. Kiểm tra thông báo lỗi
        assertEquals(
                "Vui lòng nhập lý do thực hiện",
                exception.getMessage()
        );

        // 3. Không được truy vấn hoặc lưu dữ liệu
        verifyNoInteractions(userRepo, userReviewRepo);
    }

}