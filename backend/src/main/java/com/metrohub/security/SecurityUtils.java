package com.metrohub.security;

import com.metrohub.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    

    public static CustomUserDetailsService.CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserDetails) {
            return (CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal();
        }
        
        return null;
    }

    

    public static User getCurrentUser() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getUser() : null;
    }

    

    public static Long getCurrentUserId() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getId() : null;
    }

    

    public static String getCurrentUserEmail() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getUsername() : null;
    }

    

    public static Long getCurrentUserDepartmentId() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getDepartmentId() : null;
    }

    

    public static User.UserRole getCurrentUserRole() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getRole() : null;
    }

    

    public static boolean canCurrentUserUpload() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null && userDetails.canUpload();
    }

    

    public static boolean canCurrentUserAcknowledge() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null && userDetails.canAcknowledge();
    }

    

    public static boolean canCurrentUserManageUsers() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null && userDetails.canManageUsers();
    }

    

    public static boolean hasGlobalAccess() {
        CustomUserDetailsService.CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null && userDetails.hasGlobalAccess();
    }

    

    public static boolean isSuperAdmin() {
        return getCurrentUserRole() == User.UserRole.SUPER_ADMIN;
    }

    

    public static boolean isDepartmentUploadAdmin() {
        return getCurrentUserRole() == User.UserRole.DEPARTMENT_UPLOAD_ADMIN;
    }

    

    public static boolean isDepartmentAdmin() {
        return getCurrentUserRole() == User.UserRole.DEPARTMENT_ADMIN;
    }

    

    public static boolean isDepartmentUser() {
        return getCurrentUserRole() == User.UserRole.DEPARTMENT_USER;
    }

    

    public static boolean belongsToDepartment(Long departmentId) {
        Long userDeptId = getCurrentUserDepartmentId();
        return userDeptId != null && userDeptId.equals(departmentId);
    }

    

    public static boolean canAccessDepartment(Long departmentId) {
        return hasGlobalAccess() || belongsToDepartment(departmentId);
    }

    

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
               && !"anonymousUser".equals(authentication.getPrincipal());
    }

    

    public static Long getCurrentDepartmentId() {
        return getCurrentUserDepartmentId();
    }

    

    public static boolean hasRole(String role) {
        User.UserRole currentRole = getCurrentUserRole();
        if (currentRole == null || role == null) return false;
        return currentRole.name().equals(role);
    }
}
