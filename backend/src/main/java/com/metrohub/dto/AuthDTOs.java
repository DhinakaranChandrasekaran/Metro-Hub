package com.metrohub.dto;

import com.metrohub.models.User.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class AuthDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginRequestDTO {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LoginResponseDTO {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private Long userId;
        private String name;
        private String email;
        private String employeeId;
        private String role;
        private Long departmentId;
        private String departmentName;
        private String phoneNumber;
        private String designation;
        private LocalDateTime lastLogin;
        private boolean canUpload;
        private boolean canAcknowledge;
        private boolean canManageUsers;
        private boolean hasGlobalAccess;
        @Builder.Default
        private String message = "Login successful";
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RegisterRequestDTO {
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        private String email;

        @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Please provide a valid phone number")
        private String phoneNumber;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain at least one uppercase, one lowercase, one digit and one special character"
        )
        private String password;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;

        @Size(max = 50, message = "Employee ID cannot exceed 50 characters")
        private String employeeId;

        private Long departmentId;
        private UserRole role;

        public boolean isPasswordConfirmed() {
            return password != null && password.equals(confirmPassword);
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TokenRefreshRequestDTO {
        private String refreshToken;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TokenResponseDTO {
        private String accessToken;
        private String refreshToken;
        @Builder.Default
        private String tokenType = "Bearer";
        private Long expiresIn;
        private Long refreshExpiresIn;
        private Long issuedAt;

        public static TokenResponseDTO of(String accessToken, String refreshToken, Long expiresIn) {
            return TokenResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .issuedAt(System.currentTimeMillis() / 1000)
                    .build();
        }

        public String getAuthorizationHeader() {
            return tokenType + " " + accessToken;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PasswordChangeRequestDTO {
        @NotBlank(message = "Current password is required")
        private String oldPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        private String newPassword;

        @NotBlank(message = "Password confirmation is required")
        private String confirmPassword;

        public boolean isPasswordConfirmed() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }
    }
}
