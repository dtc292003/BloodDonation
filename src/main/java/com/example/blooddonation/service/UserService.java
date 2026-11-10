
package com.example.blooddonation.service;

import com.example.blooddonation.entity.Role;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.repository.RoleRepository;
import com.example.blooddonation.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    // Fill All
    public List<User> getAllUser() {
        return userRepo.findAll();
    }

    // FIND BY ID
    public User getById(Integer id) {

        return userRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy người dùng với ID: " + id
                        )
                );
    }

    // CREATE USER
    public User createUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new RuntimeException(
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
        user.setUserStatus(UserStatus.PENDING_EMAIL);
        return userRepo.save(user);
    }

    // UPDATE USER PROFILE
    public User updateUser(Integer id, User userInfo) {
        User user = getById(id);
        user.setFullName(userInfo.getFullName());
        user.setPhoneNumber(userInfo.getPhoneNumber());
        user.setDateOfBirth(userInfo.getDateOfBirth());
        user.setGender(userInfo.getGender());
        user.setAvatarUrl(userInfo.getAvatarUrl());
        user.setAddress(userInfo.getAddress());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepo.save(user);
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

    // UPDATE USER STATUS
    public User updateUserStatus(Integer id, UserStatus status) {
        if (status == null) {
            throw new RuntimeException(
                    "Trạng thái người dùng không được để trống"
            );
        }

        User user = getById(id);
        user.setUserStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepo.save(user);
    }
}
