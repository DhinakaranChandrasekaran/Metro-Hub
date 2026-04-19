package com.metrohub.controllers;

import com.metrohub.dto.UserDTOs.*;
import com.metrohub.models.User.UserRole;
import com.metrohub.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequestDTO createRequest) {
        log.info("Creating new user: {} with role: {}", createRequest.getEmail(), createRequest.getRole());
        UserDTO createdUser = userService.createUser(createRequest);
        log.info("User created successfully: {} (ID: {})", createdUser.getName(), createdUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        return ResponseEntity.ok(userService.getAllUsers(PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/by-email")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Page<UserDTO>> getUsersByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getUsersByDepartment(departmentId, PageRequest.of(page, size)));
    }

    @GetMapping("/department/{departmentId}/list")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsersByDepartmentList(@PathVariable Long departmentId) {
        return ResponseEntity.ok(userService.getUsersByDepartmentList(departmentId));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            return ResponseEntity.ok(userService.getUsersByRole(userRole));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllDepartmentAdmins() {
        return ResponseEntity.ok(userService.getAllDepartmentAdmins());
    }

    @GetMapping("/department/{departmentId}/upload-admin")
    public ResponseEntity<UserDTO> getUploadAdmin(@PathVariable Long departmentId) {
        UserDTO admin = userService.getUploadAdminForDepartment(departmentId);
        if (admin == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(admin);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequestDTO updateRequest) {
        log.info("Updating user: {}", id);
        return ResponseEntity.ok(userService.updateUser(id, updateRequest));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok(Map.of("message", "User activated successfully"));
    }

    @GetMapping("/department/{departmentId}/notification-recipients")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<List<UserDTO>> getNotificationRecipients(@PathVariable Long departmentId) {
        return ResponseEntity.ok(userService.getNotificationRecipients(departmentId));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Long>> getUserCounts(@RequestParam(required = false) Long departmentId) {
        long count = departmentId != null ? userService.countUsersByDepartment(departmentId) : userService.countActiveUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
