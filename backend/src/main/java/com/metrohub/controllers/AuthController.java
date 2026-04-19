package com.metrohub.controllers;

import com.metrohub.dto.AuthDTOs.*;
import com.metrohub.dto.UserDTOs.UserDTO;
import com.metrohub.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        log.info("Login request for: {}", loginRequest.getEmail());
        try {
            LoginResponseDTO response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Login failed for {}: {}", loginRequest.getEmail(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        log.info("Registration request for: {}", registerRequest.getEmail());
        try {
            UserDTO response = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.warn("Registration failed for {}: {}", registerRequest.getEmail(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponseDTO> refreshToken(@RequestBody TokenRefreshRequestDTO request) {
        try {
            LoginResponseDTO response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        try {
            UserDTO currentUser = authService.getCurrentUser();
            String token = extractTokenFromRequest(request);
            authService.logout(currentUser.getId(), token);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Logged out", "status", "success"));
        }
    }

    @PostMapping("/auth/refresh-token")
    public ResponseEntity<TokenResponseDTO> refreshTokenOnly(@RequestBody TokenRefreshRequestDTO request) {
        try {
            TokenResponseDTO response = authService.refreshTokenOnly(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/auth/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        UserDTO user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old password and new password are required"));
        }
        if (confirmPassword != null && !newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password and confirmation do not match"));
        }

        UserDTO currentUser = authService.getCurrentUser();
        authService.changePassword(currentUser.getId(), oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please login again with your new password."));
    }

    @PostMapping("/auth/change-password-dto")
    public ResponseEntity<Map<String, String>> changePasswordWithDto(@Valid @RequestBody PasswordChangeRequestDTO request) {
        UserDTO currentUser = authService.getCurrentUser();
        authService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please login again with your new password."));
    }

    @PostMapping("/auth/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Token is required"));
        }
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid, "message", valid ? "Token is valid" : "Token is invalid or blacklisted"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "MetroHub API");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/health/details")
    public ResponseEntity<Map<String, Object>> healthCheckDetails() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "MetroHub API");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> components = new HashMap<>();
        components.put("database", Map.of("status", "UP", "database", "MySQL"));
        components.put("authentication", Map.of("status", "UP", "type", "JWT"));
        components.put("fileStorage", Map.of("status", "UP", "type", "Local"));
        components.put("textExtraction", Map.of("status", "UP", "service", "Apache Tika"));
        components.put("notifications", Map.of("status", "UP", "channels", "Dashboard, Email, SMS"));
        health.put("components", components);

        return ResponseEntity.ok(health);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
