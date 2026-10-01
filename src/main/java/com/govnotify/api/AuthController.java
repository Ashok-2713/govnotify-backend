package com.govnotify.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthController(
        UserRepository users,
        PasswordEncoder passwordEncoder,
        EmailService emailService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        String email = request.email().trim().toLowerCase();

        if (users.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "This email is already registered."));
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);

        // Stores an encrypted password hash, not the real password
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        users.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("message", "Account created successfully. Please log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        String email = request.email().trim().toLowerCase();

        return users.findByEmail(email)
            .filter(user -> passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
            ))
            .map(user -> ResponseEntity.ok(
    Map.of(
        "message", "Welcome, " + user.getFullName() + "!",
        "role", user.getRole(),
        "email", user.getEmail()
    )
))
            .orElseGet(() ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect email or password."))
            );
    }

    /**
     * Step 1: Generate a secure 6-digit OTP, store in database with a 10-minute expiry,
     * send via EmailService, and print to console for dev testing.
     */
    @PostMapping("/forgot-password-otp")
    public ResponseEntity<Map<String, String>> forgotPasswordOtp(
        @RequestBody Map<String, String> payload
    ) {
        String email = payload.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Email address is required."));
        }

        email = email.trim().toLowerCase();
        Optional<AppUser> userOpt = users.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No account found with this email address."));
        }

        AppUser user = userOpt.get();

        // 1. Generate secure random 6-digit OTP
        int otpNumber = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpNumber);

        // 2. Save OTP and 10-minute expiration time to database
        user.setResetOtp(otp);
        user.setResetOtpExpires(LocalDateTime.now().plusMinutes(10));
        users.save(user);

        // 3. Dev Mode Console Log for local testing
        System.out.println("==================================================");
        System.out.println("DEV OTP: " + otp + " (Sent to user: " + email + ")");
        System.out.println("==================================================");

        // 4. Send email using existing EmailService in background
        try {
            String subject = "GovNotify - Password Reset OTP";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                "You requested a password reset for your GovNotify account.\n\n" +
                "Your One-Time Password (OTP) is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes. Do not share this code with anyone.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\nGovNotify Team";

            emailService.sendJobAlert(email, subject, body);
        } catch (Exception ex) {
            System.err.println("Notice: SMTP notification failed (dev mode fallback active): " + ex.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "message", "A 6-digit OTP has been sent to your registered email address."
        ));
    }

    /**
     * Step 2: Validate OTP before user enters new password
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
        @RequestBody Map<String, String> payload
    ) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        if (email == null || otp == null || email.trim().isEmpty() || otp.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Email and OTP are required."));
        }

        email = email.trim().toLowerCase();
        Optional<AppUser> userOpt = users.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User not found."));
        }

        AppUser user = userOpt.get();

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp.trim())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Invalid OTP. Please check the 6-digit code and try again."));
        }

        if (user.getResetOtpExpires() == null || LocalDateTime.now().isAfter(user.getResetOtpExpires())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "OTP has expired. Please request a new OTP."));
        }

        return ResponseEntity.ok(Map.of("message", "OTP verified successfully."));
    }

    /**
     * Step 3: Verify OTP, hash new password, update database, and clear OTP fields.
     */
    @PostMapping("/reset-password-otp")
    public ResponseEntity<Map<String, String>> resetPasswordOtp(
        @RequestBody Map<String, String> payload
    ) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");

        if (email == null || otp == null || newPassword == null ||
            email.trim().isEmpty() || otp.trim().isEmpty() || newPassword.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Email, OTP, and new password are required."));
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Password must be at least 8 characters long."));
        }

        email = email.trim().toLowerCase();
        Optional<AppUser> userOpt = users.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User not found."));
        }

        AppUser user = userOpt.get();

        // 1. Verify OTP matches
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp.trim())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Invalid OTP code. Please enter the correct code."));
        }

        // 2. Verify OTP is not expired
        if (user.getResetOtpExpires() == null || LocalDateTime.now().isAfter(user.getResetOtpExpires())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "OTP has expired. Please request a new OTP."));
        }

        // 3. Hash the new password securely
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // 4. Clear OTP fields
        user.setResetOtp(null);
        user.setResetOtpExpires(null);
        users.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Password reset successfully! You can now log in with your new password."
        ));
    }
}