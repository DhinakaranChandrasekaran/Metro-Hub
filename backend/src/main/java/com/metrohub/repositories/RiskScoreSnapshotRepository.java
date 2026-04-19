package com.metrohub.repositories;

import com.metrohub.models.RiskScoreSnapshot;
import com.metrohub.models.RiskScoreSnapshot.EntityType;
import com.metrohub.models.RiskScoreSnapshot.RiskLevel;
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
public interface RiskScoreSnapshotRepository extends JpaRepository<RiskScoreSnapshot, Long> {

    // ============================================
    // BASIC FIND OPERATIONS
    // ============================================

    

    Optional<RiskScoreSnapshot> findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            EntityType entityType, Long entityId);

    

    List<RiskScoreSnapshot> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            EntityType entityType, Long entityId);

    

    Page<RiskScoreSnapshot> findByEntityTypeOrderByCreatedAtDesc(EntityType entityType, Pageable pageable);

    

    @Query("SELECT r FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId)")
    List<RiskScoreSnapshot> findLatestByEntityType(@Param("entityType") EntityType entityType);

    // ============================================
    // RISK LEVEL QUERIES
    // ============================================

    

    @Query("SELECT r FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.riskLevel = :riskLevel " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId)")
    List<RiskScoreSnapshot> findLatestByEntityTypeAndRiskLevel(
            @Param("entityType") EntityType entityType,
            @Param("riskLevel") RiskLevel riskLevel);

    

    @Query("SELECT r.riskLevel, COUNT(DISTINCT r.entityId) FROM RiskScoreSnapshot r " +
           "WHERE r.entityType = :entityType " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId) " +
           "GROUP BY r.riskLevel")
    List<Object[]> countByEntityTypeGroupedByRiskLevel(@Param("entityType") EntityType entityType);

    // ============================================
    // HIGH RISK QUERIES
    // ============================================

    

    @Query("SELECT r FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId) " +
           "ORDER BY r.riskScore DESC")
    List<RiskScoreSnapshot> findTopRiskEntities(@Param("entityType") EntityType entityType, Pageable pageable);

    

    @Query("SELECT r FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.riskLevel IN ('CRITICAL', 'HIGH') " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId) " +
           "ORDER BY r.riskScore DESC")
    List<RiskScoreSnapshot> findHighRiskEntities(@Param("entityType") EntityType entityType);

    // ============================================
    // TREND QUERIES
    // ============================================

    

    @Query("SELECT r FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.entityId = :entityId " +
           "AND r.createdAt >= :startDate AND r.createdAt <= :endDate " +
           "ORDER BY r.createdAt ASC")
    List<RiskScoreSnapshot> findTrendData(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Long entityId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    

    @Query(value = "SELECT DATE_FORMAT(r.created_at, '%Y-%m') as month, " +
           "AVG(r.risk_score) as avgScore, " +
           "COUNT(*) as snapshotCount " +
           "FROM risk_score_snapshots r " +
           "WHERE r.entity_type = :entityType " +
           "AND r.created_at >= :startDate " +
           "GROUP BY DATE_FORMAT(r.created_at, '%Y-%m') " +
           "ORDER BY month ASC",
           nativeQuery = true)
    List<Object[]> getMonthlyAverageScores(
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate);

    // ============================================
    // STATISTICS QUERIES
    // ============================================

    

    @Query("SELECT AVG(r.riskScore) FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId)")
    Double getAverageRiskScore(@Param("entityType") EntityType entityType);

    

    @Query("SELECT MAX(r.riskScore) FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId)")
    Integer getMaxRiskScore(@Param("entityType") EntityType entityType);

    

    @Query("SELECT MIN(r.riskScore) FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.createdAt = (SELECT MAX(r2.createdAt) FROM RiskScoreSnapshot r2 " +
           "WHERE r2.entityType = r.entityType AND r2.entityId = r.entityId)")
    Integer getMinRiskScore(@Param("entityType") EntityType entityType);

    // ============================================
    // CLEANUP QUERIES
    // ============================================

    

    long countByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    

    @Query("SELECT COUNT(r) > 0 FROM RiskScoreSnapshot r WHERE r.entityType = :entityType " +
           "AND r.entityId = :entityId AND DATE(r.createdAt) = CURRENT_DATE")
    boolean existsTodaySnapshot(@Param("entityType") EntityType entityType, @Param("entityId") Long entityId);
}
