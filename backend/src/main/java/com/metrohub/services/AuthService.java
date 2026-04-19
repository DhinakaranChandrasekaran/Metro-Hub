package com.metrohub.services;

import com.metrohub.dto.AuthDTOs.LoginRequestDTO;
import com.metrohub.dto.AuthDTOs.LoginResponseDTO;
import com.metrohub.dto.AuthDTOs.PasswordChangeRequestDTO;
import com.metrohub.dto.AuthDTOs.RegisterRequestDTO;
import com.metrohub.dto.AuthDTOs.TokenResponseDTO;
import com.metrohub.dto.UserDTOs.UserDTO;
import com.metrohub.models.Department;
import com.metrohub.models.User;
import com.metrohub.models.User.UserRole;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.UserRepository;
import com.metrohub.security.CustomUserDetailsService;
import com.metrohub.security.JwtTokenProvider;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        log.info("🔐 Login attempt for user: {}", loginRequest.getEmail());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()));

            // Get user from authentication
            CustomUserDetailsService.CustomUserDetails userDetails = (CustomUserDetailsService.CustomUserDetails) authentication
                    .getPrincipal();
            User user = userDetails.getUser();

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            log.info("✅ Login successful for user: {}", loginRequest.getEmail());

            // Build response
            return buildLoginResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            log.warn("❌ Login failed for user: {} - Invalid credentials", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public UserDTO register(RegisterRequestDTO registerRequest) {
        log.info("📝 Registration attempt for: {}", registerRequest.getEmail());

        // Validate password confirmation
        if (!registerRequest.isPasswordConfirmed()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Check if employee ID exists (if provided)
        if (registerRequest.getEmployeeId() != null &&
                userRepository.existsByEmployeeId(registerRequest.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID is already registered");
        }

        // Determine role first
        UserRole role = registerRequest.getRole() != null ? registerRequest.getRole() : UserRole.DEPARTMENT_USER;

        // Validate departmentId - required for all roles except SUPER_ADMIN
        if (role != UserRole.SUPER_ADMIN && registerRequest.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID is required for non-admin users");
        }

        // Get department
        Department department = null;
        if (registerRequest.getDepartmentId() != null) {
            Optional<Department> deptOpt = departmentRepository.findById(registerRequest.getDepartmentId());
            if (deptOpt.isEmpty()) {
                throw new IllegalArgumentException("Department not found");
            }
            department = deptOpt.get();
        }

        // Check if department already has an upload admin (if registering as one)
        if (role == UserRole.DEPARTMENT_UPLOAD_ADMIN && department != null) {
            if (userRepository.existsUploadAdminForDepartment(department.getId())) {
                throw new IllegalArgumentException(
                        "Department already has an upload admin. Only one upload admin per department is allowed.");
            }
        }

        // Create user
        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .employeeId(registerRequest.getEmployeeId())
                .role(role)
                .departmentEntity(department)
                .department(department != null ? department.getName() : null)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("✅ User registered successfully: {}", savedUser.getEmail());

        return convertToDTO(savedUser);
    }

    @Transactional
    public LoginResponseDTO refreshToken(String refreshToken) {
        log.debug("🔄 Token refresh request");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        String email = jwtTokenProvider.extractUsername(refreshToken);
        Optional<User> userOpt = userRepository.findByEmailAndIsActiveTrue(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found or inactive");
        }
        User user = userOpt.get();

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("✅ Token refreshed for user: {}", email);

        return buildLoginResponse(user, newAccessToken, newRefreshToken);
    }

        public void logout(Long userId) {
        log.info("🚪 Logout request for user ID: {}", userId);
        // Blacklist all tokens for this user
        tokenBlacklistService.blacklistAllUserTokens(userId);
        SecurityContextHolder.clearContext();
    }

        public void logout(Long userId, String token) {
        log.info("🚪 Logout request for user ID: {} with token blacklisting", userId);

        // Blacklist the specific token
        if (token != null && !token.isEmpty()) {
            long expirationSeconds = jwtTokenProvider.getExpirationInSeconds();
            tokenBlacklistService.blacklistToken(token, expirationSeconds);
        }

        SecurityContextHolder.clearContext();
        log.info("✅ User {} logged out successfully, token blacklisted", userId);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequestDTO request) {
        log.info("🔑 Password change request for user ID: {}", userId);

        // Validate password confirmation
        if (!request.isPasswordConfirmed()) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        changePassword(userId, request.getOldPassword(), request.getNewPassword());
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("🔑 Password change request for user ID: {}", userId);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Blacklist all existing tokens for this user (force re-login)
        tokenBlacklistService.blacklistAllUserTokens(userId);

        log.info("✅ Password changed successfully for user ID: {}", userId);
    }

        public boolean validateToken(String token) {
        // Check blacklist first
        if (tokenBlacklistService.isBlacklisted(token)) {
            log.debug("Token is blacklisted");
            return false;
        }
        return jwtTokenProvider.validateToken(token);
    }

        public TokenResponseDTO refreshTokenOnly(String refreshToken) {
        log.debug("🔄 Token refresh request (token only)");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Check if refresh token is blacklisted
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        String email = jwtTokenProvider.extractUsername(refreshToken);
        Optional<User> userOpt = userRepository.findByEmailAndIsActiveTrue(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found or inactive");
        }
        User user = userOpt.get();

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("✅ Token refreshed for user: {}", email);

        return TokenResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .issuedAt(System.currentTimeMillis() / 1000)
                .build();
    }

        public UserDTO getCurrentUser() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return convertToDTO(user);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private LoginResponseDTO buildLoginResponse(User user, String accessToken, String refreshToken) {
        Department department = user.getDepartmentEntity();

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .role(user.getRole().name())
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getName() : null)
                .phoneNumber(user.getPhoneNumber())
                .designation(user.getDesignation())
                .lastLogin(user.getLastLogin())
                .canUpload(user.canUpload())
                .canAcknowledge(user.canAcknowledge())
                .canManageUsers(user.canManageUsers())
                .hasGlobalAccess(user.hasGlobalAccess())
                .message("Login successful")
                .build();
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
                .designation(user.getDesignation())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .canUpload(user.canUpload())
                .canAcknowledge(user.canAcknowledge())
                .canManageUsers(user.canManageUsers())
                .hasGlobalAccess(user.hasGlobalAccess())
                .build();
    }
}
