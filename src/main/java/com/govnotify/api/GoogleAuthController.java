package com.govnotify.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class GoogleAuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    public GoogleAuthController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = new RestTemplate();
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isBlank()) {
            token = request.get("credential");
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token is required."));
        }

        try {
            String verifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenInfo = restTemplate.getForObject(verifyUrl, Map.class);

            if (tokenInfo == null || !tokenInfo.containsKey("email")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid Google token."));
            }

            String email = ((String) tokenInfo.get("email")).trim().toLowerCase();
            String name = (String) tokenInfo.get("name");
            String picture = (String) tokenInfo.get("picture");

            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            Optional<AppUser> existingUser = users.findByEmail(email);
            AppUser user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                user = new AppUser();
                user.setEmail(email);
                user.setFullName(name);
                user.setRole("USER");
                // Random password hash for OAuth user
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                user = users.save(user);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("name", user.getFullName());
            if (picture != null) {
                response.put("picture", picture);
            }
            response.put("message", "Welcome, " + user.getFullName() + "!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Google authentication failed: " + e.getMessage()));
        }
    }
}
