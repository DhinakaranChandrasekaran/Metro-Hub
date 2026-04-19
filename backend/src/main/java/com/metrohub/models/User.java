package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(name = "employee_id", unique = true, length = 50)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private UserRole role = UserRole.DEPARTMENT_USER;

    @Column(name = "department", length = 50)
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department departmentEntity;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "designation", length = 100)
    private String designation;

    public enum UserRole {
        SUPER_ADMIN,
        DEPARTMENT_UPLOAD_ADMIN,
        DEPARTMENT_ADMIN,
        DEPARTMENT_USER
    }

    public boolean canUpload() {
        return role == UserRole.SUPER_ADMIN || role == UserRole.DEPARTMENT_UPLOAD_ADMIN;
    }

    public boolean canAcknowledge() {
        return role == UserRole.SUPER_ADMIN || 
               role == UserRole.DEPARTMENT_UPLOAD_ADMIN || role == UserRole.DEPARTMENT_USER;
    }

    public boolean canManageUsers() {
        return role == UserRole.SUPER_ADMIN || role == UserRole.DEPARTMENT_ADMIN;
    }

    public boolean hasGlobalAccess() {
        return role == UserRole.SUPER_ADMIN;
    }

    public Long getDepartmentId() {
        return departmentEntity != null ? departmentEntity.getId() : null;
    }
}