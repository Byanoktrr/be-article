package com.example.IntisoftTest.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.IntisoftTest.Service.AuditLogService;
import com.example.IntisoftTest.Service.EmailService;
import com.example.IntisoftTest.Service.OtpService;
import com.example.IntisoftTest.dto.LoginRequest;
import com.example.IntisoftTest.dto.VerifyOtpRequest;
import com.example.IntisoftTest.model.User;
import com.example.IntisoftTest.repository.UserRepository;
import com.example.IntisoftTest.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String identifier = request.getIdentifier();
            String password = request.getPassword();

            Optional<User> userOpt = userRepository.findByEmail(identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(identifier);
            }

            if (userOpt.isEmpty()) {
                auditLogService.log(null, "LOGIN_FAILED", "User not found for identifier: " + identifier, getClientIp(httpRequest));
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();

            if (otpService.isBlocked(user.getEmail())) {
                auditLogService.log(user.getId(), "LOGIN_BLOCKED", "Account is blocked for 30 minutes", getClientIp(httpRequest));
                return ResponseEntity.status(403).body(Map.of("error", "Account is blocked for 30 minutes"));
            }

            if (!password.equals(user.getPassword())) {
                otpService.registerFailedAttempt(user.getEmail());
                auditLogService.log(user.getId(), "LOGIN_FAILED", "Invalid password", getClientIp(httpRequest));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
            }

            String otp = otpService.generateOtp(user.getEmail());
            emailService.sendOtpEmail(user.getEmail(), otp);
            auditLogService.log(user.getId(), "LOGIN_OTP_SENT", "OTP sent to email", getClientIp(httpRequest));

            return ResponseEntity.ok(Map.of("message", "OTP sent to email"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        try {
            String email = request.getEmail();
            String otp = request.getOtp();

            Optional<User> userOpt = userRepository.findByEmail(email);
            Long userId = userOpt.map(User::getId).orElse(null);

            if (otpService.validateOtp(email, otp)) {
                otpService.clearOtp(email);
                auditLogService.log(userId, "OTP_VERIFY_SUCCESS", "OTP verified successfully", getClientIp(httpRequest));

                String token = jwtUtil.generateToken(userOpt.get().getUsername(), userOpt.get().getRole().name());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("token", token);

                return ResponseEntity.ok(response);
            } else {
                otpService.registerFailedAttempt(email);
                auditLogService.log(userId, "OTP_VERIFY_FAILED", "Invalid or expired OTP", getClientIp(httpRequest));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired OTP"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return xfHeader == null ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }
}
