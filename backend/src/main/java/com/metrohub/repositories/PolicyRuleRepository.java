package com.metrohub.repositories;

import com.metrohub.models.Document;
import com.metrohub.models.PolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    // ============================================
    // FIND ALL POLICIES
    // ============================================

    List<PolicyRule> findByIsActiveTrueOrderByDepartmentAscPriorityAsc();

    List<PolicyRule> findAllByOrderByDepartmentAscPriorityAsc();

    // ============================================
    // FIND BY DEPARTMENT
    // ============================================

    

    @Query("SELECT p FROM PolicyRule p WHERE p.department.id = :departmentId AND p.isActive = true")
    List<PolicyRule> findByDepartmentIdAndIsActiveTrue(@Param("departmentId") Long departmentId);
    

    @Query("SELECT p FROM PolicyRule p WHERE p.department.id = :departmentId")
    List<PolicyRule> findByDepartmentId(@Param("departmentId") Long departmentId);

    // ============================================
    // EXACT MATCH QUERIES
    // ============================================

    

    @Query("SELECT p FROM PolicyRule p LEFT JOIN FETCH p.department LEFT JOIN FETCH p.createdBy LEFT JOIN FETCH p.updatedBy " +
           "WHERE p.department.id = :departmentId AND p.priority = :priority AND p.isActive = true")
    Optional<PolicyRule> findByDepartmentIdAndPriorityActive(
            @Param("departmentId") Long departmentId,
            @Param("priority") Document.Priority priority);

    

    @Query("SELECT p FROM PolicyRule p LEFT JOIN FETCH p.department WHERE p.department.id = :departmentId AND p.priority = :priority")
    Optional<PolicyRule> findByDepartmentIdAndPriority(
            @Param("departmentId") Long departmentId, 
            @Param("priority") Document.Priority priority);

    // ============================================
    // FALLBACK QUERIES
    // ============================================

    

    @Query("SELECT p FROM PolicyRule p LEFT JOIN FETCH p.department LEFT JOIN FETCH p.createdBy LEFT JOIN FETCH p.updatedBy " +
           "WHERE p.department.id = :departmentId AND p.priority IS NULL AND p.isActive = true")
    Optional<PolicyRule> findByDepartmentIdAndNullPriorityActive(@Param("departmentId") Long departmentId);

    

    @Query("SELECT p FROM PolicyRule p LEFT JOIN FETCH p.department LEFT JOIN FETCH p.createdBy LEFT JOIN FETCH p.updatedBy " +
           "WHERE p.department IS NULL AND p.priority = :priority AND p.isActive = true")
    Optional<PolicyRule> findByNullDepartmentAndPriorityActive(@Param("priority") Document.Priority priority);

    

    @Query("SELECT p FROM PolicyRule p LEFT JOIN FETCH p.department LEFT JOIN FETCH p.createdBy LEFT JOIN FETCH p.updatedBy " +
           "WHERE p.department IS NULL AND p.priority IS NULL AND p.isDefault = true AND p.isActive = true")
    Optional<PolicyRule> findGlobalDefaultPolicy();

    

    @Query("SELECT p FROM PolicyRule p LEFT JOIN FETCH p.department LEFT JOIN FETCH p.createdBy LEFT JOIN FETCH p.updatedBy " +
           "WHERE p.isDefault = true")
    Optional<PolicyRule> findByIsDefaultTrue();

    // ============================================
    // HIERARCHICAL POLICY LOOKUP
    // ============================================

    

    @Query("SELECT p FROM PolicyRule p WHERE p.isActive = true AND " +
           "((p.department.id = :departmentId AND p.priority = :priority) OR " +
           "(p.department.id = :departmentId AND p.priority IS NULL) OR " +
           "(p.department IS NULL AND p.priority = :priority) OR " +
           "(p.department IS NULL AND p.priority IS NULL AND p.isDefault = true)) " +
           "ORDER BY " +
           "CASE WHEN p.department.id = :departmentId AND p.priority = :priority THEN 1 " +
           "WHEN p.department.id = :departmentId AND p.priority IS NULL THEN 2 " +
           "WHEN p.department IS NULL AND p.priority = :priority THEN 3 " +
           "ELSE 4 END")
    List<PolicyRule> findMatchingPolicies(
            @Param("departmentId") Long departmentId,
            @Param("priority") Document.Priority priority);

    // ============================================
    // EXISTENCE CHECKS
    // ============================================

    

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PolicyRule p " +
           "WHERE p.department.id = :departmentId AND p.priority = :priority")
    boolean existsByDepartmentIdAndPriority(
            @Param("departmentId") Long departmentId, 
            @Param("priority") Document.Priority priority);

    

    boolean existsByIsDefaultTrue();

    // ============================================
    // COUNT QUERIES
    // ============================================

    

    long countByIsActiveTrue();

    

    @Query("SELECT COUNT(p) FROM PolicyRule p WHERE p.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);
}
