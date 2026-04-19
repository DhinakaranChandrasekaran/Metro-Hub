package com.metrohub.repositories;

import com.metrohub.models.User;
import com.metrohub.models.User.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================
    // BASIC FIND OPERATIONS
    // ============================================

    

    Optional<User> findByEmail(String email);

    

    Optional<User> findByEmployeeId(String employeeId);

    

    Optional<User> findByEmailAndIsActiveTrue(String email);

    // ============================================
    // DEPARTMENT-BASED QUERIES (Phase 6)
    // ============================================

    

    @Query("SELECT u FROM User u WHERE u.departmentEntity.id = :departmentId AND u.isActive = true")
    List<User> findByDepartmentIdAndActive(@Param("departmentId") Long departmentId);

    

    @Query("SELECT u FROM User u WHERE u.departmentEntity.id = :departmentId")
    Page<User> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    

    @Query("SELECT u FROM User u WHERE u.departmentEntity.id = :departmentId AND u.role = :role AND u.isActive = true")
    List<User> findByDepartmentIdAndRole(
            @Param("departmentId") Long departmentId,
            @Param("role") UserRole role
    );

    

    @Query("SELECT u FROM User u WHERE u.departmentEntity.id = :departmentId AND u.role = 'DEPARTMENT_UPLOAD_ADMIN' AND u.isActive = true")
    Optional<User> findUploadAdminByDepartmentId(@Param("departmentId") Long departmentId);

    

    @Query("SELECT u FROM User u WHERE u.departmentEntity.id = :departmentId " +
           "AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_USER') " +
           "AND u.isActive = true")
    List<User> findNotificationRecipientsForDepartment(@Param("departmentId") Long departmentId);

    

    @Query("SELECT u FROM User u WHERE u.departmentEntity.id = :departmentId " +
           "AND u.phoneNumber IS NOT NULL AND u.phoneNumber != '' AND u.isActive = true")
    List<User> findUsersWithPhoneInDepartment(@Param("departmentId") Long departmentId);

    // ============================================
    // ROLE-BASED QUERIES (Phase 6)
    // ============================================

    

    List<User> findByRole(UserRole role);

    

    List<User> findByRoleAndIsActiveTrue(UserRole role);

    

    Page<User> findByRole(UserRole role, Pageable pageable);

    

    @Query("SELECT u FROM User u WHERE u.role = 'SUPER_ADMIN' AND u.isActive = true")
    List<User> findAllSuperAdmins();

    

    @Query("SELECT u FROM User u WHERE u.role IN ('DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN') AND u.isActive = true")
    List<User> findAllDepartmentAdmins();

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    

    @Query("SELECT COUNT(u) FROM User u WHERE u.departmentEntity.id = :departmentId AND u.isActive = true")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    

    long countByRole(UserRole role);

    

    long countByIsActiveTrue();

    // ============================================
    // VALIDATION QUERIES
    // ============================================

    

    boolean existsByEmail(String email);

    

    boolean existsByEmployeeId(String employeeId);

    

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.departmentEntity.id = :departmentId " +
           "AND u.role = 'DEPARTMENT_UPLOAD_ADMIN' AND u.isActive = true")
    boolean existsUploadAdminForDepartment(@Param("departmentId") Long departmentId);

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :timestamp WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);

    

    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.id = :userId")
    void deactivateUser(@Param("userId") Long userId);

    

    @Modifying
    @Query("UPDATE User u SET u.isActive = true WHERE u.id = :userId")
    void activateUser(@Param("userId") Long userId);
}
