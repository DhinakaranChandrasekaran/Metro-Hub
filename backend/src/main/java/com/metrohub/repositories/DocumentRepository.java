package com.metrohub.repositories;

import com.metrohub.models.Document;
import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Fetch extracted text directly (avoids @Lob lazy-loading)
    @Query("SELECT d.extractedText FROM Document d WHERE d.id = :id")
    String findExtractedTextById(@Param("id") Long id);

    // Find by single field

    Page<Document> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<Document> findByDocumentType(DocumentType documentType, Pageable pageable);

    Page<Document> findByPriority(Priority priority, Pageable pageable);

    Page<Document> findByUploadedById(Long userId, Pageable pageable);

    // Search queries

    @Query(value = "SELECT * FROM documents d WHERE " +
           "LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT COUNT(*) FROM documents d WHERE " +
           "LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           nativeQuery = true)
    Page<Document> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM documents d WHERE " +
           "(:keyword IS NULL OR LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.department_id = :departmentId) AND " +
           "(:documentType IS NULL OR d.document_type = :documentType) AND " +
           "(:priority IS NULL OR d.priority = :priority) AND " +
           "(:dateFrom IS NULL OR d.upload_date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR d.upload_date <= :dateTo)",
           countQuery = "SELECT COUNT(*) FROM documents d WHERE " +
           "(:keyword IS NULL OR LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.department_id = :departmentId) AND " +
           "(:documentType IS NULL OR d.document_type = :documentType) AND " +
           "(:priority IS NULL OR d.priority = :priority) AND " +
           "(:dateFrom IS NULL OR d.upload_date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR d.upload_date <= :dateTo)",
           nativeQuery = true)
    Page<Document> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("documentType") String documentType,
            @Param("priority") String priority,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    // Count queries for dashboard

    long countByDepartmentId(Long departmentId);

    long countByDocumentType(DocumentType documentType);

    @Query("SELECT COUNT(d) FROM Document d WHERE DATE(d.uploadDate) = CURRENT_DATE")
    long countTodayUploads();

    long countByStatus(Document.DocumentStatus status);

    long countByPriority(Priority priority);

    long countByDepartmentIdAndPriority(Long departmentId, Priority priority);

    // Dashboard queries

    Page<Document> findAllByOrderByUploadDateDesc(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.uploadDate >= :since ORDER BY d.uploadDate DESC")
    List<Document> findRecentDocuments(@Param("since") LocalDateTime since, Pageable pageable);

    @Query(value = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline IS NOT NULL",
           nativeQuery = true)
    long countDocumentsWithDeadlines();

    @Query(value = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "LEFT JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE (d.priority = 'HIGH' OR (m.deadline IS NOT NULL AND m.deadline >= CURRENT_DATE)) " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    long countPendingActions();

    @Query(value = "SELECT d.* FROM documents d " +
           "LEFT JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE (d.priority = 'HIGH' OR (m.deadline IS NOT NULL AND m.deadline >= CURRENT_DATE)) " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED' " +
           "ORDER BY d.priority DESC, m.deadline ASC, d.upload_date DESC",
           countQuery = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "LEFT JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE (d.priority = 'HIGH' OR (m.deadline IS NOT NULL AND m.deadline >= CURRENT_DATE)) " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    Page<Document> findPendingActionDocuments(Pageable pageable);

    // Deadline tracking queries

    @Query(value = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline < CURRENT_DATE " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    long countOverdueDocuments();

    @Query(value = "SELECT d.* FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline < CURRENT_DATE " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED' " +
           "ORDER BY m.deadline ASC",
           countQuery = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline < CURRENT_DATE " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    Page<Document> findOverdueDocuments(Pageable pageable);

    @Query(value = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline >= CURRENT_DATE " +
           "AND m.deadline <= DATE_ADD(CURRENT_DATE, INTERVAL :days DAY) " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    long countDocumentsDueSoon(@Param("days") int days);

    @Query(value = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline >= :startDate AND m.deadline <= :endDate " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    long countDocumentsDueInRange(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT d.* FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline >= :startDate AND m.deadline <= :endDate " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED' " +
           "ORDER BY m.deadline ASC",
           countQuery = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline >= :startDate AND m.deadline <= :endDate " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    Page<Document> findDocumentsDueInRange(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate, 
                                           Pageable pageable);

    @Query(value = "SELECT d.* FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline IS NOT NULL " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED' " +
           "ORDER BY m.deadline ASC",
           countQuery = "SELECT COUNT(DISTINCT d.id) FROM documents d " +
           "INNER JOIN document_metadata m ON d.id = m.document_id " +
           "WHERE m.deadline IS NOT NULL " +
           "AND d.status != 'ARCHIVED' AND d.status != 'DELETED'",
           nativeQuery = true)
    Page<Document> findDocumentsWithDeadlines(Pageable pageable);

    // Enhanced search queries

    @Query(value = "SELECT * FROM documents d WHERE " +
           "LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT COUNT(*) FROM documents d WHERE " +
           "LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           nativeQuery = true)
    Page<Document> searchByKeywordWithSummary(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM documents d WHERE " +
           "(:keyword IS NULL OR LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.department_id = :departmentId) AND " +
           "(:documentType IS NULL OR d.document_type = :documentType) AND " +
           "(:priority IS NULL OR d.priority = :priority) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:dateFrom IS NULL OR d.upload_date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR d.upload_date <= :dateTo)",
           countQuery = "SELECT COUNT(*) FROM documents d WHERE " +
           "(:keyword IS NULL OR LOWER(d.file_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(d.extracted_text AS CHAR(10000))) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.department_id = :departmentId) AND " +
           "(:documentType IS NULL OR d.document_type = :documentType) AND " +
           "(:priority IS NULL OR d.priority = :priority) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:dateFrom IS NULL OR d.upload_date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR d.upload_date <= :dateTo)",
           nativeQuery = true)
    Page<Document> searchWithFiltersAndStatus(
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("documentType") String documentType,
            @Param("priority") String priority,
            @Param("status") String status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    // Compliance scheduler queries

    @Query("SELECT DISTINCT d FROM Document d " +
           "LEFT JOIN FETCH d.department " +
           "WHERE d.status = 'ACTIVE' " +
           "AND d.uploadDate <= :threshold " +
           "AND d.department IS NOT NULL " +
           "AND EXISTS (SELECT u FROM User u " +
           "            WHERE u.departmentEntity.id = d.department.id " +
           "            AND u.isActive = true " +
           "            AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER') " +
           "            AND u.id NOT IN (SELECT da.user.id FROM DocumentAcknowledgement da WHERE da.document.id = d.id))")
    List<Document> findDocumentsNeedingComplianceCheck(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT DISTINCT d FROM Document d " +
           "WHERE d.status = 'ACTIVE' " +
           "AND d.uploadDate <= :threshold " +
           "AND d.department.id = :departmentId " +
           "AND EXISTS (SELECT u FROM User u " +
           "            WHERE u.departmentEntity.id = d.department.id " +
           "            AND u.isActive = true " +
           "            AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER') " +
           "            AND u.id NOT IN (SELECT da.user.id FROM DocumentAcknowledgement da WHERE da.document.id = d.id))")
    List<Document> findDocumentsNeedingComplianceCheckByDepartment(
            @Param("threshold") LocalDateTime threshold,
            @Param("departmentId") Long departmentId
    );

    @Query("SELECT COUNT(DISTINCT d) FROM Document d " +
           "WHERE d.status = 'ACTIVE' " +
           "AND d.department.id = :departmentId " +
           "AND EXISTS (SELECT u FROM User u " +
           "            WHERE u.departmentEntity.id = d.department.id " +
           "            AND u.isActive = true " +
           "            AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER') " +
           "            AND u.id NOT IN (SELECT da.user.id FROM DocumentAcknowledgement da WHERE da.document.id = d.id))")
    long countDocumentsWithPendingAcknowledgements(@Param("departmentId") Long departmentId);

    @Query("SELECT COUNT(DISTINCT d) FROM Document d " +
           "WHERE d.status = 'ACTIVE' " +
           "AND d.uploadDate <= :threshold " +
           "AND d.department IS NOT NULL " +
           "AND EXISTS (SELECT u FROM User u " +
           "            WHERE u.departmentEntity.id = d.department.id " +
           "            AND u.isActive = true " +
           "            AND u.role IN ('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER') " +
           "            AND u.id NOT IN (SELECT da.user.id FROM DocumentAcknowledgement da WHERE da.document.id = d.id))")
    long countOverdueUnacknowledgedDocuments(@Param("threshold") LocalDateTime threshold);

    // Audit and compliance report queries

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = 'ACTIVE'")
    long countActiveDocuments();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadDate >= :startDate AND d.uploadDate <= :endDate")
    long countDocumentsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.documentType = :docType AND d.uploadDate >= :startDate AND d.uploadDate <= :endDate")
    long countDocumentsByTypeInDateRange(
            @Param("docType") Document.DocumentType docType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.documentType IN ('SAFETY_CIRCULAR', 'LEGAL_NOTICE') AND d.status = 'ACTIVE'")
    long countSafetyDocuments();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.department.id = :departmentId AND d.uploadDate >= :startDate AND d.uploadDate <= :endDate")
    long countDocumentsByDepartmentInDateRange(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d.documentType as docType, COUNT(d) as docCount FROM Document d WHERE d.status = 'ACTIVE' GROUP BY d.documentType")
    List<Object[]> countDocumentsGroupedByType();

    @Query("SELECT d.department.id as deptId, d.department.name as deptName, COUNT(d) as docCount " +
           "FROM Document d WHERE d.status = 'ACTIVE' AND d.department IS NOT NULL GROUP BY d.department.id, d.department.name")
    List<Object[]> countDocumentsGroupedByDepartment();

    // Legal hold queries

    List<Document> findByLegalHoldTrue();

    List<Document> findByDepartmentIdAndLegalHoldTrue(Long departmentId);

    long countByLegalHoldTrue();

    long countByDepartmentIdAndLegalHoldTrue(Long departmentId);

    Page<Document> findByLegalHoldTrue(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.uploadDate IS NOT NULL " +
           "AND d.uploadDate < :threshold " +
           "AND (d.isSlaManual IS NULL OR d.isSlaManual = false) " +
           "AND d.slaConfiguredAt IS NULL " +
           "AND d.status = 'ACTIVE'")
    List<Document> findDocumentsNeedingAutoSla(@Param("threshold") LocalDateTime threshold);

    // Delete related FK records for cascade delete
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM document_reminders WHERE document_id = :docId", nativeQuery = true)
    void deleteRemindersByDocumentId(@Param("docId") Long docId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM compliance_violations WHERE document_id = :docId", nativeQuery = true)
    void deleteViolationsByDocumentId(@Param("docId") Long docId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM alerts WHERE document_id = :docId", nativeQuery = true)
    void deleteAlertsByDocumentId(@Param("docId") Long docId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM audit_logs WHERE entity_type = 'Document' AND entity_id = :docId", nativeQuery = true)
    void deleteAuditLogsByDocumentId(@Param("docId") Long docId);
}