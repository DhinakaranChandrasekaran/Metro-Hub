package com.metrohub.dto;

import com.metrohub.models.User.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class UserDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDTO {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private String employeeId;
        private UserRole role;
        private String roleName;
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
        private boolean canUpload;
        private boolean canAcknowledge;
        private boolean canManageUsers;
        private boolean hasGlobalAccess;
        private String designation;

        public String getRoleDescription() {
            if (role == null) return "Unknown";
            return switch (role) {
                case SUPER_ADMIN -> "Super Administrator - Global system oversight";
                case DEPARTMENT_UPLOAD_ADMIN -> "Department Upload Admin - Upload & acknowledge documents";
                case DEPARTMENT_ADMIN -> "Department Manager - Manage department users";
                case DEPARTMENT_USER -> "Department User - View & acknowledge documents";
            };
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateUserRequestDTO {
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

        @Size(max = 50, message = "Employee ID cannot exceed 50 characters")
        private String employeeId;

        @NotNull(message = "Department ID is required")
        private Long departmentId;

        @NotNull(message = "Role is required")
        private UserRole role;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserUpdateRequestDTO {
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @Email(message = "Please provide a valid email")
        private String email;

        @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Please provide a valid phone number")
        private String phoneNumber;

        @Size(max = 50, message = "Employee ID cannot exceed 50 characters")
        private String employeeId;

        private Long departmentId;
        private UserRole role;
        private Boolean isActive;
        private String designation;
    }
}
