
package com.example.blooddonation.dto;

import com.example.blooddonation.enums.Gender;
import com.example.blooddonation.enums.RoleName;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 64,
                message = "Mật khẩu phải từ 8 đến 64 ký tự")
        String password,

        @NotBlank(message = "Số điện thoại không được để trống")
        String phoneNumber,

        @NotNull(message = "Ngày sinh không được để trống")
        @Past(message = "Ngày sinh phải trong quá khứ")
        LocalDate dateOfBirth,

        @NotNull(message = "Giới tính không được để trống")
        Gender gender,

        String address,

        @NotNull(message = "Vui lòng chọn loại tài khoản")
        RoleName roleName
) {}
