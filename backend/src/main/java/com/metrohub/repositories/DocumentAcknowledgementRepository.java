package com.metrohub.repositories;

import com.metrohub.models.DocumentAcknowledgement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentAcknowledgementRepository extends JpaRepository<DocumentAcknowledgement, Long> {

    // ============================================
    // FIND OPERATIONS
    // ============================================

    Optional<DocumentAcknowledgement> findByDocument_IdAndUser_Id(Long documentId, Long userId);

    List<DocumentAcknowledgement> findByDocument_IdOrderByAcknowledgedAtDesc(Long documentId);

    List<DocumentAcknowledgement> findByUser_IdOrderByAcknowledgedAtDesc(Long userId);

    Page<DocumentAcknowledgement> findByUser_Id(Long userId, Pageable pageable);

    Page<DocumentAcknowledgement> findByDocument_Id(Long documentId, Pageable pageable);

    @Query("SELECT da FROM DocumentAcknowledgement da " +
           "WHERE da.acknowledgedAt >= :since " +
           "ORDER BY da.acknowledgedAt DESC")
    List<DocumentAcknowledgement> findRecentAcknowledgements(@Param("since") LocalDateTime since);

    @Query("SELECT da FROM DocumentAcknowledgement da " +
           "WHERE da.document.department.id = :departmentId " +
           "ORDER BY da.acknowledgedAt DESC")
    List<DocumentAcknowledgement> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT da FROM DocumentAcknowledgement da " +
           "WHERE da.document.department.id = :departmentId " +
           "ORDER BY da.acknowledgedAt DESC")
    Page<DocumentAcknowledgement> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    // ============================================
    // EXISTS CHECKS
    // ============================================

    boolean existsByDocument_IdAndUser_Id(Long documentId, Long userId);

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    long countByDocument_Id(Long documentId);

    long countByUser_Id(Long userId);

    @Query("SELECT COUNT(u) FROM User u " +
           "WHERE u.departmentEntity.id = (SELECT d.department.id FROM Document d WHERE d.id = :documentId) " +
           "AND u.isActive = true " +
           "AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER')")
    long countExpectedAcknowledgements(@Param("documentId") Long documentId);

    @Query("SELECT COUNT(u) FROM User u " +
           "WHERE u.departmentEntity.id = (SELECT d.department.id FROM Document d WHERE d.id = :documentId) " +
           "AND u.isActive = true " +
           "AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER') " +
           "AND u.id NOT IN (SELECT da.user.id FROM DocumentAcknowledgement da WHERE da.document.id = :documentId)")
    long countPendingAcknowledgements(@Param("documentId") Long documentId);

    @Query("SELECT COUNT(d) FROM Document d " +
           "WHERE d.department.id = :departmentId " +
           "AND d.status = 'ACTIVE' " +
           "AND d.id NOT IN (SELECT da.document.id FROM DocumentAcknowledgement da WHERE da.user.id = :userId)")
    long countPendingForUser(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    @Query("SELECT COUNT(da) FROM DocumentAcknowledgement da " +
           "WHERE da.document.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    // ============================================
    // COMPLIANCE QUERIES
    // ============================================

    @Query("SELECT u FROM User u " +
           "WHERE u.departmentEntity.id = (SELECT d.department.id FROM Document d WHERE d.id = :documentId) " +
           "AND u.isActive = true " +
           "AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER') " +
           "AND u.id NOT IN (SELECT da.user.id FROM DocumentAcknowledgement da WHERE da.document.id = :documentId)")
    List<com.metrohub.models.User> findUsersNotAcknowledged(@Param("documentId") Long documentId);

    @Query("SELECT d FROM com.metrohub.models.Document d " +
           "WHERE d.department.id = :departmentId " +
           "AND d.status = 'ACTIVE' " +
           "AND d.id NOT IN (SELECT da.document.id FROM DocumentAcknowledgement da WHERE da.user.id = :userId) " +
           "ORDER BY d.uploadDate DESC")
    List<com.metrohub.models.Document> findDocumentsPendingAcknowledgementByUser(
            @Param("userId") Long userId,
            @Param("departmentId") Long departmentId
    );

    @Query("SELECT d FROM com.metrohub.models.Document d " +
           "WHERE d.department.id = :departmentId " +
           "AND d.status = 'ACTIVE' " +
           "AND d.id NOT IN (SELECT da.document.id FROM DocumentAcknowledgement da WHERE da.user.id = :userId)")
    Page<com.metrohub.models.Document> findDocumentsPendingAcknowledgementByUser(
            @Param("userId") Long userId,
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );

    // ============================================
    // STATISTICS QUERIES
    // ============================================

    @Query("SELECT COALESCE(CAST(COUNT(da) AS double) * 100.0 / NULLIF(" +
           "(SELECT COUNT(u) FROM User u WHERE u.departmentEntity.id = d.department.id " +
           "AND u.isActive = true AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER')), 0), 0) " +
           "FROM Document d LEFT JOIN DocumentAcknowledgement da ON d.id = da.document.id " +
           "WHERE d.id = :documentId " +
           "GROUP BY d.id, d.department.id")
    Double getAcknowledgementRate(@Param("documentId") Long documentId);

    @Query("SELECT new map(" +
           "COUNT(DISTINCT d.id) as totalDocuments, " +
           "COUNT(DISTINCT da.document.id) as acknowledgedDocuments) " +
           "FROM Document d LEFT JOIN DocumentAcknowledgement da ON d.id = da.document.id " +
           "WHERE d.department.id = :departmentId AND d.status = 'ACTIVE'")
    java.util.Map<String, Long> getDepartmentAcknowledgementStats(@Param("departmentId") Long departmentId);

    // ============================================
    // PHASE 8: AUDIT & COMPLIANCE REPORT QUERIES
    // ============================================

    @Query("SELECT COUNT(da) FROM DocumentAcknowledgement da")
    long countTotalAcknowledgements();

    @Query("SELECT COUNT(da) FROM DocumentAcknowledgement da WHERE da.acknowledgedAt >= :startDate AND da.acknowledgedAt <= :endDate")
    long countAcknowledgementsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(da) FROM DocumentAcknowledgement da WHERE da.user.id = :userId")
    long countAcknowledgementsByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(da) FROM DocumentAcknowledgement da WHERE da.user.id = :userId AND da.acknowledgedAt >= :startDate AND da.acknowledgedAt <= :endDate")
    long countAcknowledgementsByUserInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate); 

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u " +
           "INNER JOIN Document d ON d.department.id = u.departmentEntity.id " +
           "WHERE u.isActive = true " +
           "AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER') " +
           "AND d.status = 'ACTIVE'")
    long countTotalExpectedAcknowledgements();

    @Query("SELECT da FROM DocumentAcknowledgement da WHERE da.document.id = :documentId ORDER BY da.acknowledgedAt ASC")
    List<DocumentAcknowledgement> findAllByDocumentIdOrderByAcknowledgedAt(@Param("documentId") Long documentId);

    @Query("SELECT MAX(da.acknowledgedAt) FROM DocumentAcknowledgement da WHERE da.user.id = :userId")
    LocalDateTime getLastAcknowledgementDateByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(da) FROM DocumentAcknowledgement da " +
           "WHERE da.user.id = :userId " +
           "AND da.document.department.id = :departmentId")
    long countDocumentsAcknowledgedByUserInDepartment(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    @Query("SELECT COUNT(d) FROM Document d " +
           "WHERE d.department.id = :departmentId " +
           "AND d.status = 'ACTIVE'")
    long countTotalDocumentsAssignedToUser(@Param("departmentId") Long departmentId);

    // Delete all acknowledgements for a document (for cascade delete)
    void deleteByDocument_Id(Long documentId);
}

