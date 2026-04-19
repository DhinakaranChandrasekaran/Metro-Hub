package com.metrohub.repositories;

import com.metrohub.models.Alert;
import com.metrohub.models.Alert.AlertType;
import com.metrohub.models.Alert.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // ============================================
    // FIND OPERATIONS (Existing)
    // ============================================

    

    Page<Alert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    

    Page<Alert> findByIsReadFalseOrderByCreatedAtDesc(Pageable pageable);

    

    List<Alert> findByDocument_IdOrderByCreatedAtDesc(Long documentId);

    

    Page<Alert> findByAlertTypeOrderByCreatedAtDesc(AlertType alertType, Pageable pageable);

    

    Page<Alert> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable
    );

    // ============================================
    // USER-TARGETED QUERIES (Phase 6)
    // ============================================

    

    @Query("SELECT a FROM Alert a WHERE a.targetUser.id = :userId ORDER BY a.createdAt DESC")
    Page<Alert> findByTargetUserId(@Param("userId") Long userId, Pageable pageable);

    

    @Query("SELECT a FROM Alert a WHERE a.targetUser.id = :userId AND a.isRead = false ORDER BY a.createdAt DESC")
    Page<Alert> findUnreadByTargetUserId(@Param("userId") Long userId, Pageable pageable);

    

    @Query("SELECT a FROM Alert a WHERE a.targetUser.id = :userId ORDER BY a.createdAt DESC")
    List<Alert> findAllByTargetUserId(@Param("userId") Long userId);

    

    @Query("SELECT a FROM Alert a WHERE a.targetUser.id = :userId AND a.isRead = false ORDER BY a.createdAt DESC")
    List<Alert> findUnreadListByTargetUserId(@Param("userId") Long userId);

    // ============================================
    // DEPARTMENT QUERIES (Phase 6)
    // ============================================

    

    @Query("SELECT a FROM Alert a WHERE a.department.id = :departmentId ORDER BY a.createdAt DESC")
    Page<Alert> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    

    @Query("SELECT a FROM Alert a WHERE a.department.id = :departmentId ORDER BY a.createdAt DESC")
    Page<Alert> findByDepartmentIdOrderByCreatedAtDesc(@Param("departmentId") Long departmentId, Pageable pageable);

    

    @Query("SELECT a FROM Alert a WHERE a.department.id = :departmentId AND a.isRead = false ORDER BY a.createdAt DESC")
    Page<Alert> findUnreadByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    

    @Query("SELECT a FROM Alert a WHERE a.targetUser.id = :userId OR a.department.id = :departmentId ORDER BY a.createdAt DESC")
    Page<Alert> findByTargetUserIdOrDepartmentIdOrderByCreatedAtDesc(
            @Param("userId") Long userId, 
            @Param("departmentId") Long departmentId, 
            Pageable pageable);

    

    @Query("SELECT a FROM Alert a WHERE (a.targetUser.id = :userId OR a.department.id = :departmentId) AND a.isRead = false ORDER BY a.createdAt DESC")
    Page<Alert> findUnreadByUserOrDepartment(
            @Param("userId") Long userId, 
            @Param("departmentId") Long departmentId, 
            Pageable pageable);

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    

    long countByIsReadFalse();

    

    long countByAlertTypeAndIsReadFalse(AlertType alertType);

    

    long countByDocument_Id(Long documentId);

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.targetUser.id = :userId AND a.isRead = false")
    long countUnreadByTargetUserId(@Param("userId") Long userId);

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.department.id = :departmentId AND a.isRead = false")
    long countUnreadByDepartmentId(@Param("departmentId") Long departmentId);

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE (a.targetUser.id = :userId OR a.department.id = :departmentId) AND a.isRead = false")
    long countUnreadByUserOrDepartment(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.isRead = false")
    long countAllUnread();

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.emailSent = false AND a.targetUser.email IS NOT NULL")
    long countPendingEmailAlerts();

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.smsSent = false AND a.targetUser.phoneNumber IS NOT NULL")
    long countPendingSmsAlerts();

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.isRead = false")
    int markAllAsRead();

    

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.document.id = :documentId")
    int markAsReadByDocumentId(@Param("documentId") Long documentId);

    

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.targetUser.id = :userId AND a.isRead = false")
    int markAllAsReadByTargetUserId(@Param("userId") Long userId);

    

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE (a.targetUser.id = :userId OR a.department.id = :departmentId) AND a.isRead = false")
    int markAsReadByUserOrDepartment(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    

    @Modifying
    @Query("UPDATE Alert a SET a.emailSent = true WHERE a.id = :alertId")
    void markEmailSent(@Param("alertId") Long alertId);

    

    @Modifying
    @Query("UPDATE Alert a SET a.smsSent = true WHERE a.id = :alertId")
    void markSmsSent(@Param("alertId") Long alertId);

    // ============================================
    // DUPLICATE CHECK OPERATIONS
    // ============================================

    

    @Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.document.id = :documentId " +
           "AND a.alertType = :alertType AND a.createdAt >= :since")
    boolean existsByDocumentIdAndAlertTypeSince(
            @Param("documentId") Long documentId,
            @Param("alertType") AlertType alertType,
            @Param("since") LocalDateTime since
    );

    

    @Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.targetUser.id = :userId " +
           "AND a.document.id = :documentId AND a.alertType = :alertType AND a.createdAt >= :since")
    boolean existsByUserIdAndDocumentIdAndAlertTypeSince(
            @Param("userId") Long userId,
            @Param("documentId") Long documentId,
            @Param("alertType") AlertType alertType,
            @Param("since") LocalDateTime since
    );

    

    @Query("SELECT a FROM Alert a WHERE a.document.id = :documentId AND a.alertType = :alertType")
    List<Alert> findByDocumentIdAndAlertType(@Param("documentId") Long documentId, @Param("alertType") AlertType alertType);

    // ============================================
    // NOTIFICATION CHANNEL QUERIES (Phase 6)
    // ============================================

    

    @Query("SELECT a FROM Alert a WHERE a.emailSent = false AND a.targetUser.email IS NOT NULL")
    List<Alert> findPendingEmailAlerts();

    

    @Query("SELECT a FROM Alert a WHERE a.smsSent = false AND a.targetUser.phoneNumber IS NOT NULL")
    List<Alert> findPendingSmsAlerts();

    

    Page<Alert> findByNotificationChannel(NotificationChannel channel, Pageable pageable);

    // ============================================
    // CLEANUP OPERATIONS
    // ============================================

    

    @Modifying
    @Query("DELETE FROM Alert a WHERE a.isRead = true AND a.createdAt < :before")
    int deleteOldReadAlerts(@Param("before") LocalDateTime before);

    

    void deleteByDocument_Id(Long documentId);

    // ============================================
    // PHASE 8: AUDIT & COMPLIANCE REPORT QUERIES
    // ============================================

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.document.id = :documentId")
    long countAlertsByDocument(@Param("documentId") Long documentId);

    

    @Query("SELECT a FROM Alert a WHERE a.document.id = :documentId ORDER BY a.createdAt ASC")
    List<Alert> findAllByDocumentIdOrderByCreatedAt(@Param("documentId") Long documentId);

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.document.id = :documentId AND a.alertType = 'COMPLIANCE_REMINDER'")
    long countRemindersSentForDocument(@Param("documentId") Long documentId);

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.document.id = :documentId AND a.alertType = 'ESCALATION_DEPT_ADMIN'")
    long countDeptAdminEscalationsForDocument(@Param("documentId") Long documentId);

    

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.document.id = :documentId AND a.alertType = 'ESCALATION_SUPER_ADMIN'")
    long countSuperAdminEscalationsForDocument(@Param("documentId") Long documentId);
}

