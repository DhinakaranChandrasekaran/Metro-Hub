package com.metrohub.services;

import com.metrohub.dto.UserDTOs.CreateUserRequestDTO;
import com.metrohub.dto.UserDTOs.UserDTO;
import com.metrohub.dto.UserDTOs.UserUpdateRequestDTO;
import com.metrohub.models.Department;
import com.metrohub.models.User;
import com.metrohub.models.User.UserRole;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.UserRepository;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    

    @Transactional
    public UserDTO createUser(CreateUserRequestDTO request) {
        log.info("📝 Creating new user: {} with role: {}", request.getEmail(), request.getRole());
        
        // Validate unique constraints
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        if (request.getEmployeeId() != null && userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }
        
        // Get department
        Optional<Department> deptOpt = departmentRepository.findById(request.getDepartmentId());
        if (deptOpt.isEmpty()) {
            throw new IllegalArgumentException("Department not found: " + request.getDepartmentId());
        }
        Department department = deptOpt.get();
        
        // Validate role hierarchy
        validateRoleHierarchy(request.getRole(), request.getDepartmentId());
        
        // Check DEPARTMENT_UPLOAD_ADMIN uniqueness per department
        if (request.getRole() == UserRole.DEPARTMENT_UPLOAD_ADMIN) {
            if (userRepository.existsUploadAdminForDepartment(request.getDepartmentId())) {
                throw new IllegalArgumentException(
                    "Department '" + department.getName() + "' already has an Upload Admin. " +
                    "Each department can have only one Upload Admin.");
            }
        }
        
        // Create user
        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phoneNumber(request.getPhoneNumber())
            .employeeId(request.getEmployeeId())
            .role(request.getRole())
            .departmentEntity(department)
            .department(department.getName())
            .isActive(true)
            .build();
        
        User savedUser = userRepository.save(user);
        log.info("✅ User created successfully: {} (ID: {}, Role: {})", 
            savedUser.getName(), savedUser.getId(), savedUser.getRole());
        
        return convertToDTO(savedUser);
    }

    

    private void validateRoleHierarchy(UserRole targetRole, Long departmentId) {
        UserRole currentUserRole = SecurityUtils.getCurrentUserRole();
        Long currentUserDeptId = SecurityUtils.getCurrentUserDepartmentId();
        
        log.debug("🔐 Validating role hierarchy: {} trying to create {}", currentUserRole, targetRole);
        
        switch (currentUserRole) {
            case SUPER_ADMIN:
                // SUPER_ADMIN can only create DEPARTMENT_ADMIN
                if (targetRole != UserRole.DEPARTMENT_ADMIN) {
                    throw new AccessDeniedException(
                        "SUPER_ADMIN can only create DEPARTMENT_ADMIN users. " +
                        "To create " + targetRole + ", ask the Department Admin.");
                }
                break;
                
            case DEPARTMENT_ADMIN:
                // DEPARTMENT_ADMIN can only create in their own department
                if (!departmentId.equals(currentUserDeptId)) {
                    throw new AccessDeniedException(
                        "You can only create users in your own department.");
                }
                // DEPARTMENT_ADMIN can create DEPARTMENT_UPLOAD_ADMIN or DEPARTMENT_USER
                if (targetRole != UserRole.DEPARTMENT_UPLOAD_ADMIN && 
                    targetRole != UserRole.DEPARTMENT_USER) {
                    throw new AccessDeniedException(
                        "DEPARTMENT_ADMIN can only create DEPARTMENT_UPLOAD_ADMIN or DEPARTMENT_USER roles.");
                }
                break;
                
            default:
                throw new AccessDeniedException(
                    "Only SUPER_ADMIN and DEPARTMENT_ADMIN can create users.");
        }
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        User user = userOpt.get();
        
        // Check access
        validateAccessToUser(user);
        
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }
        User user = userOpt.get();
        
        validateAccessToUser(user);
        
        return convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users");
        
        // Only SUPER_ADMIN can view all users
        if (!SecurityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Only SUPER_ADMIN can view all users");
        }
        
        return userRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByDepartment(Long departmentId, Pageable pageable) {
        log.debug("Fetching users for department: {}", departmentId);
        
        // Validate access to department
        validateDepartmentAccess(departmentId);
        
        return userRepository.findByDepartmentId(departmentId, pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByDepartmentList(Long departmentId) {
        log.debug("Fetching users list for department: {}", departmentId);
        
        validateDepartmentAccess(departmentId);
        
        return userRepository.findByDepartmentIdAndActive(departmentId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(UserRole role) {
        log.debug("Fetching users by role: {}", role);
        
        // Only SUPER_ADMIN can query by role
        if (!SecurityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Only SUPER_ADMIN can query users by role");
        }
        
        return userRepository.findByRoleAndIsActiveTrue(role).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(Long id, UserUpdateRequestDTO updateRequest) {
        log.info("Updating user: {}", id);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();
        
        // Validate access
        validateAccessToUser(user);
        
        // Update fields if provided
        if (updateRequest.getName() != null) {
            user.setName(updateRequest.getName());
        }
        
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new IllegalArgumentException("Email is already in use");
            }
            user.setEmail(updateRequest.getEmail());
        }
        
        if (updateRequest.getPhoneNumber() != null) {
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        
        if (updateRequest.getEmployeeId() != null && !updateRequest.getEmployeeId().equals(user.getEmployeeId())) {
            if (userRepository.existsByEmployeeId(updateRequest.getEmployeeId())) {
                throw new IllegalArgumentException("Employee ID is already in use");
            }
            user.setEmployeeId(updateRequest.getEmployeeId());
        }
        
        // Only SUPER_ADMIN can change department and role
        if (SecurityUtils.isSuperAdmin()) {
            if (updateRequest.getDepartmentId() != null) {
                Optional<Department> deptOpt = departmentRepository.findById(updateRequest.getDepartmentId());
                if (deptOpt.isEmpty()) {
                    throw new IllegalArgumentException("Department not found");
                }
                Department department = deptOpt.get();
                user.setDepartmentEntity(department);
                user.setDepartment(department.getName());
            }
            
            if (updateRequest.getRole() != null) {
                // Check if setting as upload admin and department already has one
                if (updateRequest.getRole() == UserRole.DEPARTMENT_UPLOAD_ADMIN && 
                    user.getDepartmentId() != null) {
                    if (userRepository.existsUploadAdminForDepartment(user.getDepartmentId()) &&
                        user.getRole() != UserRole.DEPARTMENT_UPLOAD_ADMIN) {
                        throw new IllegalArgumentException("Department already has an upload admin");
                    }
                }
                user.setRole(updateRequest.getRole());
            }
            
            if (updateRequest.getIsActive() != null) {
                user.setIsActive(updateRequest.getIsActive());
            }
        }

        // Designation can be updated by any user for their own profile
        if (updateRequest.getDesignation() != null) {
            user.setDesignation(updateRequest.getDesignation());
        }
        
        User savedUser = userRepository.save(user);
        log.info("✅ User updated: {}", savedUser.getId());
        
        return convertToDTO(savedUser);
    }

    @Transactional
    public void deactivateUser(Long id) {
        log.info("Deactivating user: {}", id);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();
        
        validateAccessToUser(user);
        
        // Cannot deactivate yourself
        if (id.equals(SecurityUtils.getCurrentUserId())) {
            throw new IllegalArgumentException("Cannot deactivate your own account");
        }
        
        userRepository.deactivateUser(id);
        log.info("✅ User deactivated: {}", id);
    }

    @Transactional
    public void activateUser(Long id) {
        log.info("Activating user: {}", id);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();
        
        validateAccessToUser(user);
        
        userRepository.activateUser(id);
        log.info("✅ User activated: {}", id);
    }

    @Transactional(readOnly = true)
    public UserDTO getUploadAdminForDepartment(Long departmentId) {
        log.debug("Fetching upload admin for department: {}", departmentId);
        
        return userRepository.findUploadAdminByDepartmentId(departmentId)
            .map(this::convertToDTO)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllDepartmentAdmins() {
        log.debug("Fetching all department admins");
        
        return userRepository.findAllDepartmentAdmins().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getNotificationRecipients(Long departmentId) {
        log.debug("Fetching notification recipients for department: {}", departmentId);
        
        return userRepository.findNotificationRecipientsForDepartment(departmentId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

        public long countUsersByDepartment(Long departmentId) {
        return userRepository.countByDepartmentId(departmentId);
    }

        public long countActiveUsers() {
        return userRepository.countByIsActiveTrue();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private void validateAccessToUser(User user) {
        // SUPER_ADMIN can access anyone
        if (SecurityUtils.isSuperAdmin()) {
            return;
        }
        
        // Users can access their own profile
        if (user.getId().equals(SecurityUtils.getCurrentUserId())) {
            return;
        }
        
        // DEPARTMENT_ADMIN can access users in their department
        if (SecurityUtils.isDepartmentAdmin() || SecurityUtils.isDepartmentUploadAdmin()) {
            Long currentDeptId = SecurityUtils.getCurrentUserDepartmentId();
            Long userDeptId = user.getDepartmentId();
            if (currentDeptId != null && currentDeptId.equals(userDeptId)) {
                return;
            }
        }
        
        throw new AccessDeniedException("You don't have permission to access this user");
    }

    private void validateDepartmentAccess(Long departmentId) {
        if (!SecurityUtils.canAccessDepartment(departmentId)) {
            throw new AccessDeniedException("You don't have access to this department");
        }
    }

    private UserDTO convertToDTO(User user) {
        Department department = user.getDepartmentEntity();
        
        return UserDTO.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .phoneNumber(user.getPhoneNumber())
            .employeeId(user.getEmployeeId())
            .role(user.getRole())
            .roleName(user.getRole().name())
            .departmentId(department != null ? department.getId() : null)
            .departmentName(department != null ? department.getName() : null)
            .departmentCode(department != null ? department.getCode() : null)
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt())
            .lastLogin(user.getLastLogin())
            .canUpload(user.canUpload())
            .canAcknowledge(user.canAcknowledge())
            .canManageUsers(user.canManageUsers())
            .hasGlobalAccess(user.hasGlobalAccess())
            .designation(user.getDesignation())
            .build();
    }
}
