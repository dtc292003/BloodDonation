
package com.example.blooddonation.service;

import com.example.blooddonation.dto.ResendOtpRequest;
import com.example.blooddonation.enums.RoleName;
import com.example.blooddonation.dto.VerifyEmailRequest;
import com.example.blooddonation.entity.EmailOTP;
import com.example.blooddonation.entity.User;
import com.example.blooddonation.enums.OtpPurpose;
import com.example.blooddonation.enums.UserStatus;
import com.example.blooddonation.exception.InvalidOtpException;
import com.example.blooddonation.exception.ResourceNotFoundException;
import com.example.blooddonation.repository.EmailOTPRepository;
import com.example.blooddonation.repository.UserRepository;

import com.nimbusds.oauth2.sdk.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final EmailOTPRepository emailOtpRepo;
    private final UserRepository userRepo;
    private final OtpGenerator otpGenerator;
    private final OtpHashService otpHashService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createAndSendOtp(Integer userId) {

        // 1. Tìm USER
        User user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy người dùng"
                        )
                );

        // 2. Kiểm tra trạng thái
        if (user.getUserStatus() != UserStatus.PENDING_EMAIL ||
                Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException(
                    "Tài khoản không cần xác minh email"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // 3. Giới hạn gửi lại 60 giây
        emailOtpRepo
                .findFirstByUser_UserIdAndPurposeOrderByCreatedAtDescOtpIdDesc(
                        userId,
                        OtpPurpose.EMAIL_VERIFICATION
                )
                .ifPresent(lastOtp -> {
                    if (lastOtp.getCreatedAt()
                            .plusSeconds(60).isAfter(now)) {
                        throw new IllegalArgumentException(
                                "Vui lòng chờ 60 giây trước khi gửi lại OTP"
                        );
                    }
                });

        // 4. Sinh mã OTP
        String rawOtp = otpGenerator.generateOtp();

        // 5. Tạo bản ghi EMAIL_OTP
        EmailOTP emailOTP = new EmailOTP();

        emailOTP.setUser(user);
        emailOTP.setPurpose(OtpPurpose.EMAIL_VERIFICATION);

        emailOTP.setOtpHash(
                otpHashService.hash(
                        userId,
                        OtpPurpose.EMAIL_VERIFICATION,
                        rawOtp
                )
        );

        emailOTP.setCreatedAt(now);
        emailOTP.setExpiresAt(now.plusMinutes(5));
        emailOTP.setAttemptCount((short) 0);
        emailOTP.setUsedAt(null);

        // 6. Lưu OTP vào MySQL
        emailOtpRepo.save(emailOTP);

        // 7. Yêu cầu gửi email sau khi transaction commit
        eventPublisher.publishEvent(
                new OtpMailEvent(user.getEmail(), rawOtp)
        );
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void verifyEmail(VerifyEmailRequest request) {

        // 1. Chuẩn hóa email
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2. Tìm người dùng
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException(
                        "Email hoặc mã OTP không hợp lệ"
                ));

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getUserStatus() != UserStatus.PENDING_EMAIL ||
                Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new InvalidOtpException(
                    "Tài khoản không ở trạng thái chờ xác minh email"
            );
        }

        // 4. Tìm OTP mới nhất dành cho xác minh email
        EmailOTP emailOTP = emailOtpRepo
                .findFirstByUser_UserIdAndPurposeOrderByCreatedAtDescOtpIdDesc(
                        user.getUserId(),
                        OtpPurpose.EMAIL_VERIFICATION
                )
                .orElseThrow(() -> new InvalidOtpException(
                        "Không tìm thấy mã OTP"
                ));

        LocalDateTime now = LocalDateTime.now();

        // 5. OTP đã được sử dụng
        if (emailOTP.getUsedAt() != null) {
            throw new InvalidOtpException(
                    "Mã OTP đã được sử dụng"
            );
        }

        // 6. OTP hết hạn
        if (!emailOTP.getExpiresAt().isAfter(now)) {
            throw new InvalidOtpException(
                    "Mã OTP đã hết hạn"
            );
        }

        // 7. Kiểm tra số lần nhập sai
        if (emailOTP.getAttemptCount() >= 5) {
            throw new InvalidOtpException(
                    "Đã vượt quá 5 lần thử, vui lòng yêu cầu mã mới"
            );
        }

        // 8. Tính lại HMAC từ OTP người dùng nhập
        String inputHash = otpHashService.hash(
                user.getUserId(),
                OtpPurpose.EMAIL_VERIFICATION,
                request.otp()
        );

        // 9. So sánh bản băm
        boolean matched = MessageDigest.isEqual(
                Base64.getDecoder().decode(emailOTP.getOtpHash()),
                Base64.getDecoder().decode(inputHash)
        );

        if (!matched) {

            // Tăng số lần nhập sai
            emailOTP.setAttemptCount(
                    (short) (emailOTP.getAttemptCount() + 1)
            );

            emailOtpRepo.save(emailOTP);

            throw new InvalidOtpException(
                    "Mã OTP không đúng"
            );
        }

        // 10. OTP đúng -> đánh dấu đã sử dụng
        emailOTP.setUsedAt(now);
        emailOtpRepo.save(emailOTP);

        // 11. Kích hoạt tài khoản theo vai trò
        user.setEmailVerified(true);

        if (user.getRole().getRoleName() == RoleName.USER) {
            user.setUserStatus(UserStatus.ACTIVE);

        } else if (user.getRole().getRoleName() == RoleName.MEDICAL) {
            user.setUserStatus(UserStatus.PENDING_APPROVAL);

        } else {
            throw new IllegalStateException(
                    "Không thể xác minh tài khoản với vai trò này"
            );
        }

        user.setUpdatedAt(now);

        // 12. Lưu trạng thái người dùng
        userRepo.save(user);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);
        Optional<User> optionalUser = userRepo.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return;
        }
        User user = optionalUser.get();

        if (user.getUserStatus() != UserStatus.PENDING_EMAIL || Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        Optional<EmailOTP> latestOtp = emailOtpRepo
                .findFirstByUser_UserIdAndPurposeOrderByCreatedAtDescOtpIdDesc(
                        user.getUserId(),
                        OtpPurpose.EMAIL_VERIFICATION
                );
        if (latestOtp.isPresent()) {

            LocalDateTime nextAllowedTime = latestOtp.get()
                    .getCreatedAt()
                    .plusSeconds(60);

            if (LocalDateTime.now().isBefore(nextAllowedTime)) {
                return;
            }
        }createAndSendOtp(user.getUserId());

    }
}
