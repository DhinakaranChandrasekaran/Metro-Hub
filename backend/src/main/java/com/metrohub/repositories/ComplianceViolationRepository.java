package com.metrohub.repositories;

import com.metrohub.models.ComplianceViolation;
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
public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, Long> {

    // ============================================
    // BASIC FIND OPERATIONS
    // ============================================

    

    Optional<ComplianceViolation> findByDocument_IdAndUser_Id(Long documentId, Long userId);

    

    List<ComplianceViolation> findByDocument_IdOrderByCreatedAtDesc(Long documentId);

    

    List<ComplianceViolation> findByUser_IdOrderByCreatedAtDesc(Long userId);

    

    List<ComplianceViolation> findByDepartment_IdOrderByCreatedAtDesc(Long departmentId);

    // ============================================
    // PAGINATED QUERIES
    // ============================================

    

    Page<ComplianceViolation> findByUser_Id(Long userId, Pageable pageable);

    

    Page<ComplianceViolation> findByDepartment_Id(Long departmentId, Pageable pageable);

    

    Page<ComplianceViolation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    

    @Query("SELECT cv FROM ComplianceViolation cv WHERE cv.resolved = false ORDER BY cv.createdAt DESC")
    Page<ComplianceViolation> findPendingViolations(Pageable pageable);

    

    @Query("SELECT cv FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.resolved = false ORDER BY cv.createdAt DESC")
    Page<ComplianceViolation> findPendingViolationsByDepartment(@Param("departmentId") Long departmentId, Pageable pageable);

    

    @Query("SELECT cv FROM ComplianceViolation cv WHERE cv.user.id = :userId AND cv.resolved = false ORDER BY cv.createdAt DESC")
    Page<ComplianceViolation> findPendingViolationsByUser(@Param("userId") Long userId, Pageable pageable);

    // ============================================
    // EXISTS CHECKS
    // ============================================

    

    boolean existsByDocument_IdAndUser_Id(Long documentId, Long userId);

    

    @Query("SELECT COUNT(cv) > 0 FROM ComplianceViolation cv WHERE cv.user.id = :userId AND cv.resolved = false")
    boolean hasUserPendingViolations(@Param("userId") Long userId);

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    

    long countByUser_Id(Long userId);

    

    long countByDepartment_Id(Long departmentId);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.user.id = :userId AND cv.resolved = false")
    long countPendingByUserId(@Param("userId") Long userId);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.resolved = false")
    long countPendingByDepartmentId(@Param("departmentId") Long departmentId);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.resolved = true")
    long countResolvedByDepartmentId(@Param("departmentId") Long departmentId);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.acknowledgedLate = true")
    long countLateAcknowledgedByDepartmentId(@Param("departmentId") Long departmentId);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.resolved = false")
    long countAllPending();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.resolved = true")
    long countAllResolved();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.daysDelayed >= :minDays AND cv.daysDelayed < :maxDays AND cv.resolved = false")
    long countBySeverityRange(@Param("minDays") int minDays, @Param("maxDays") int maxDays);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.daysDelayed >= 14 AND cv.resolved = false")
    long countCriticalViolations();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.daysDelayed >= 14 AND cv.resolved = false")
    long countCriticalByDepartmentId(@Param("departmentId") Long departmentId);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.daysDelayed >= 10 AND cv.daysDelayed < 14 AND cv.resolved = false")
    long countHighSeverityViolations();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.daysDelayed >= 10 AND cv.daysDelayed < 14 AND cv.resolved = false")
    long countHighByDepartmentId(@Param("departmentId") Long departmentId);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.daysDelayed >= 7 AND cv.daysDelayed < 10 AND cv.resolved = false")
    long countMediumSeverityViolations();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.daysDelayed >= 7 AND cv.daysDelayed < 10 AND cv.resolved = false")
    long countMediumByDepartmentId(@Param("departmentId") Long departmentId);

    // ============================================
    // TIME-BASED QUERIES
    // ============================================

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.createdAt >= :since")
    long countViolationsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.user.id = :userId AND cv.createdAt >= :since")
    long countViolationsSinceByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.createdAt >= :since")
    long countViolationsSinceByDepartment(@Param("departmentId") Long departmentId, @Param("since") LocalDateTime since);

    // ============================================
    // ESCALATION TRACKING QUERIES
    // ============================================

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.reminderSent = true")
    long countRemindersSent();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.deptAdminEscalated = true")
    long countDeptAdminEscalations();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.superAdminEscalated = true")
    long countSuperAdminEscalations();

    // ============================================
    // DEPARTMENT STATISTICS QUERIES
    // ============================================

    

    @Query("SELECT cv.department.id, COUNT(cv) FROM ComplianceViolation cv " +
           "WHERE cv.resolved = false GROUP BY cv.department.id")
    List<Object[]> countPendingGroupedByDepartment();

    

    @Query("SELECT cv.department.id, cv.department.name, COUNT(cv) as violationCount " +
           "FROM ComplianceViolation cv WHERE cv.resolved = false " +
           "GROUP BY cv.department.id, cv.department.name " +
           "ORDER BY violationCount DESC")
    List<Object[]> findHighRiskDepartments(Pageable pageable);

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    

    @Modifying
    @Query("UPDATE ComplianceViolation cv SET cv.acknowledgedLate = true, cv.lateAcknowledgementDate = :acknowledgedAt WHERE cv.id = :violationId")
    void markLateAcknowledged(@Param("violationId") Long violationId, @Param("acknowledgedAt") LocalDateTime acknowledgedAt);

    

    @Modifying
    @Query("UPDATE ComplianceViolation cv SET cv.reminderSent = true, cv.reminderSentAt = :sentAt WHERE cv.id = :violationId")
    void markReminderSent(@Param("violationId") Long violationId, @Param("sentAt") LocalDateTime sentAt);

    

    @Modifying
    @Query("UPDATE ComplianceViolation cv SET cv.deptAdminEscalated = true, cv.deptAdminEscalatedAt = :escalatedAt WHERE cv.id = :violationId")
    void markDeptAdminEscalated(@Param("violationId") Long violationId, @Param("escalatedAt") LocalDateTime escalatedAt);

    

    @Modifying
    @Query("UPDATE ComplianceViolation cv SET cv.superAdminEscalated = true, cv.superAdminEscalatedAt = :escalatedAt WHERE cv.id = :violationId")
    void markSuperAdminEscalated(@Param("violationId") Long violationId, @Param("escalatedAt") LocalDateTime escalatedAt);

    // ============================================
    // PHASE 8: AUDIT & COMPLIANCE REPORT QUERIES
    // ============================================

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.createdAt >= :startDate AND cv.createdAt <= :endDate")
    long countViolationsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.resolved = true AND cv.resolvedDate >= :startDate AND cv.resolvedDate <= :endDate")
    long countResolvedInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.createdAt >= :startDate AND cv.createdAt <= :endDate")
    long countViolationsByDepartmentInDateRange(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.document.documentType IN ('SAFETY_CIRCULAR', 'LEGAL_NOTICE')")
    long countSafetyViolations();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.document.documentType IN ('SAFETY_CIRCULAR', 'LEGAL_NOTICE') AND cv.resolved = false")
    long countUnresolvedSafetyViolations();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.acknowledgedLate = true")
    long countLateAcknowledged();

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.user.id = :userId AND cv.acknowledgedLate = true")
    long countLateAcknowledgedByUser(@Param("userId") Long userId);

    

    @Query("SELECT cv.user.id, cv.user.name, cv.user.employeeId, cv.department.name, COUNT(cv) as violationCount " +
           "FROM ComplianceViolation cv " +
           "GROUP BY cv.user.id, cv.user.name, cv.user.employeeId, cv.department.name " +
           "HAVING COUNT(cv) >= :minViolations " +
           "ORDER BY violationCount DESC")
    List<Object[]> findDefaulters(@Param("minViolations") long minViolations, Pageable pageable);

    

    @Query("SELECT cv.user.id, COUNT(cv) as violationCount " +
           "FROM ComplianceViolation cv " +
           "WHERE cv.createdAt >= :since " +
           "GROUP BY cv.user.id " +
           "HAVING COUNT(cv) >= :minViolations")
    List<Object[]> findRepeatDefaulters(@Param("since") LocalDateTime since, @Param("minViolations") long minViolations);

    

    @Query("SELECT COUNT(cv) FROM ComplianceViolation cv WHERE cv.user.id = :userId AND cv.createdAt >= :startDate AND cv.createdAt <= :endDate")
    long countUserViolationsInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    

    @Query(value = "SELECT DATE_FORMAT(cv.created_at, '%Y-%m') as month, COUNT(*) as count " +
           "FROM compliance_violations cv " +
           "WHERE cv.created_at >= :startDate " +
           "GROUP BY DATE_FORMAT(cv.created_at, '%Y-%m') " +
           "ORDER BY month ASC",
           nativeQuery = true)
    List<Object[]> getMonthlyViolationCounts(@Param("startDate") LocalDateTime startDate);

    

    @Query(value = "SELECT DATE_FORMAT(cv.resolved_date, '%Y-%m') as month, COUNT(*) as count " +
           "FROM compliance_violations cv " +
           "WHERE cv.resolved = true AND cv.resolved_date >= :startDate " +
           "GROUP BY DATE_FORMAT(cv.resolved_date, '%Y-%m') " +
           "ORDER BY month ASC",
           nativeQuery = true)
    List<Object[]> getMonthlyResolvedCounts(@Param("startDate") LocalDateTime startDate);

    

    @Query("SELECT cv FROM ComplianceViolation cv WHERE cv.document.id = :documentId ORDER BY cv.createdAt DESC")
    List<ComplianceViolation> findAllByDocumentId(@Param("documentId") Long documentId);

    

    @Query("SELECT AVG(cv.daysDelayed) FROM ComplianceViolation cv WHERE cv.resolved = false")
    Double getAverageDaysDelayed();

    

    @Query("SELECT AVG(cv.daysDelayed) FROM ComplianceViolation cv WHERE cv.department.id = :departmentId AND cv.resolved = false")
    Double getAverageDaysDelayedByDepartment(@Param("departmentId") Long departmentId);

    

    @Query("SELECT MAX(cv.daysDelayed) FROM ComplianceViolation cv WHERE cv.resolved = false")
    Integer getMaxDaysDelayed();

    

    @Query(value = "SELECT " +
           "CASE " +
           "  WHEN days_delayed BETWEEN 7 AND 9 THEN '7-9 days' " +
           "  WHEN days_delayed BETWEEN 10 AND 13 THEN '10-13 days' " +
           "  WHEN days_delayed BETWEEN 14 AND 20 THEN '14-20 days' " +
           "  ELSE '21+ days' " +
           "END as delay_range, " +
           "COUNT(*) as count " +
           "FROM compliance_violations " +
           "WHERE resolved = false " +
           "GROUP BY delay_range " +
           "ORDER BY MIN(days_delayed)",
           nativeQuery = true)
    List<Object[]> getDelayDistribution();
}

