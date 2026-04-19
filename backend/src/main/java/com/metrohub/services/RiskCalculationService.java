package com.metrohub.services;

import com.metrohub.dto.AnalyticsDTOs.DepartmentRiskDTO;
import com.metrohub.dto.AnalyticsDTOs.UserRiskDTO;
import com.metrohub.models.ComplianceViolation;
import com.metrohub.models.Department;
import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.RiskScoreSnapshot;
import com.metrohub.models.RiskScoreSnapshot.EntityType;
import com.metrohub.models.RiskScoreSnapshot.RiskLevel;
import com.metrohub.models.User;
import com.metrohub.repositories.ComplianceViolationRepository;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.DocumentAcknowledgementRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.repositories.RiskScoreSnapshotRepository;
import com.metrohub.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RiskCalculationService {

    private static final Logger log = LoggerFactory.getLogger(RiskCalculationService.class);

    private final ComplianceViolationRepository violationRepository;
    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RiskScoreSnapshotRepository snapshotRepository;

    // ============================================
    // RISK FACTOR WEIGHTS
    // ============================================

    private static final int WEIGHT_LATE_ACK = 10;
    private static final int MAX_LATE_ACK_POINTS = 20;

    private static final int WEIGHT_PENDING_VIOLATION = 15;
    private static final int MAX_PENDING_VIOLATION_POINTS = 45;

    private static final int WEIGHT_DEPT_ESCALATION = 8;
    private static final int MAX_DEPT_ESCALATION_POINTS = 16;

    private static final int WEIGHT_SUPER_ESCALATION = 12;
    private static final int MAX_SUPER_ESCALATION_POINTS = 24;

    private static final int WEIGHT_LEGAL_HOLD = 10;
    private static final int MAX_LEGAL_HOLD_POINTS = 20;

    private static final int WEIGHT_SAFETY_VIOLATION = 15;
    private static final int MAX_SAFETY_VIOLATION_POINTS = 30;

    private static final int WEIGHT_REPEAT_OFFENSE = 5;
    private static final int MAX_REPEAT_OFFENSE_POINTS = 15;

    // ============================================
    // USER RISK CALCULATION
    // ============================================

    

    @Transactional(readOnly = true)
    public UserRiskDTO calculateUserRisk(Long userId) {
        log.info("📊 Calculating risk score for user ID: {}", userId);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + userId);
        }
        User user = userOpt.get();

        Long departmentId = user.getDepartmentEntity() != null ? user.getDepartmentEntity().getId() : null;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last30Days = now.minusDays(30);
        LocalDateTime last90Days = now.minusDays(90);

        // Gather risk factors
        int lateAckCount = (int) violationRepository.countLateAcknowledgedByUser(userId);
        int totalViolations = (int) violationRepository.countByUser_Id(userId);
        int pendingViolations = (int) violationRepository.countPendingByUserId(userId);

        // Get escalation counts from violations
        List<ComplianceViolation> userViolations =
                violationRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        int deptAdminEscalations = (int) userViolations.stream()
                .filter(this::isDeptAdminEscalated)
                .count();
        int superAdminEscalations = (int) userViolations.stream()
                .filter(this::isSuperAdminEscalated)
                .count();

        // Safety violations (documents with type SAFETY_CIRCULAR or LEGAL_NOTICE)
        int safetyViolations = (int) userViolations.stream()
                .filter(this::isSafetyRelatedViolation)
                .count();

        // Legal hold violations
        int legalHoldViolations = (int) userViolations.stream()
                .filter(this::isLegalHoldViolation)
                .count();

        // Repeat offenses (violations in last 90 days after first violation)
        int recentViolations = (int) violationRepository.countViolationsSinceByUser(userId, last90Days);
        int repeatOffenses = Math.max(0, totalViolations - 1);  // All after first are repeats
        if (recentViolations > 0) {
            repeatOffenses = Math.max(repeatOffenses, recentViolations - 1);
        }

        // Calculate risk score
        int riskScore = calculateScore(
                lateAckCount, pendingViolations, deptAdminEscalations,
                superAdminEscalations, legalHoldViolations, safetyViolations, repeatOffenses
        );

        RiskLevel riskLevel = RiskScoreSnapshot.calculateRiskLevel(riskScore);

        // Build compliance metrics
        long acknowledgedDocs = departmentId != null ?
                acknowledgementRepository.countDocumentsAcknowledgedByUserInDepartment(userId, departmentId) : 0;
        long totalDocs = departmentId != null ?
                acknowledgementRepository.countTotalDocumentsAssignedToUser(departmentId) : 0;
        long pendingAcks = totalDocs - acknowledgedDocs;
        double complianceRate = totalDocs > 0 ? (acknowledgedDocs * 100.0 / totalDocs) : 100.0;

        // Trend calculation
        int violations7Days = (int) violationRepository.countViolationsSinceByUser(userId, now.minusDays(7));
        int violations30Days = (int) violationRepository.countViolationsSinceByUser(userId, last30Days);
        String trendDirection = calculateTrend(violations7Days, violations30Days);

        // Defaulter flags
        boolean isChronicDefaulter = totalViolations >= 5;
        boolean isRepeatOffender = violations30Days >= 3;
        boolean hasOpenEscalation = userViolations.stream()
                .anyMatch(this::isOpenEscalation);

        return UserRiskDTO.builder()
                .userId(userId)
                .userName(user.getName())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .role(user.getRole().name())
                .departmentId(departmentId)
                .departmentName(user.getDepartmentEntity() != null ? user.getDepartmentEntity().getName() : null)
                .departmentCode(user.getDepartmentEntity() != null ? user.getDepartmentEntity().getCode() : null)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .riskLevelColor(getRiskLevelColor(riskLevel))
                .riskLevelDescription(getRiskLevelDescription(riskLevel))
                .lateAcknowledgementCount(lateAckCount)
                .totalViolationCount(totalViolations)
                .pendingViolationCount(pendingViolations)
                .resolvedViolationCount(totalViolations - pendingViolations)
                .deptAdminEscalationCount(deptAdminEscalations)
                .superAdminEscalationCount(superAdminEscalations)
                .legalHoldDocumentCount(legalHoldViolations)
                .safetyViolationCount(safetyViolations)
                .complianceRate(complianceRate)
                .totalAssignedDocuments((int) totalDocs)
                .acknowledgedDocuments((int) acknowledgedDocs)
                .pendingAcknowledgements((int) pendingAcks)
                .violationsLast7Days(violations7Days)
                .violationsLast30Days(violations30Days)
                .violationsLast90Days((int) violationRepository.countViolationsSinceByUser(userId, last90Days))
                .trendDirection(trendDirection)
                .isChronicDefaulter(isChronicDefaulter)
                .isRepeatOffender(isRepeatOffender)
                .hasOpenEscalation(hasOpenEscalation)
                .hasLegalHoldViolation(legalHoldViolations > 0)
                .lastCalculatedAt(now)
                .periodStart(now.minusDays(90))
                .periodEnd(now)
                .build();
    }

    // ============================================
    // DEPARTMENT RISK CALCULATION
    // ============================================

    

    @Transactional(readOnly = true)
    public DepartmentRiskDTO calculateDepartmentRisk(Long departmentId) {
        log.info("📊 Calculating risk score for department ID: {}", departmentId);

        Optional<Department> deptOpt = departmentRepository.findById(departmentId);
        if (deptOpt.isEmpty()) {
            throw new RuntimeException("Department not found: " + departmentId);
        }
        Department department = deptOpt.get();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7Days = now.minusDays(7);
        LocalDateTime last30Days = now.minusDays(30);

        // Gather department-level risk factors
        int totalViolations = (int) violationRepository.countByDepartment_Id(departmentId);
        int pendingViolations = (int) violationRepository.countPendingByDepartmentId(departmentId);
        int lateAckCount = (int) violationRepository.countLateAcknowledgedByDepartmentId(departmentId);

        // Get violation details for escalation counts
        List<ComplianceViolation> deptViolations =
                violationRepository.findByDepartment_IdOrderByCreatedAtDesc(departmentId);

        int deptAdminEscalations = (int) deptViolations.stream()
                .filter(this::isDeptAdminEscalated)
                .count();
        int superAdminEscalations = (int) deptViolations.stream()
                .filter(this::isSuperAdminEscalated)
                .count();

        // Safety violations
        int safetyViolations = (int) deptViolations.stream()
                .filter(this::isSafetyRelatedViolation)
                .count();

        // Legal hold documents
        long legalHoldDocs = documentRepository.countByDepartmentIdAndLegalHoldTrue(departmentId);

        // Legal hold violations (adds to legal hold docs count for scoring)
        int legalHoldViolationCount = (int) deptViolations.stream()
                .filter(this::isLegalHoldViolation)
                .count();
        // Combine legal hold docs and legal hold violations for risk calculation
        long totalLegalHoldImpact = legalHoldDocs + legalHoldViolationCount;

        // Repeat offenders in department
        List<User> deptUsers = userRepository.findByDepartmentIdAndActive(departmentId);
        int repeatOffenderCount = 0;
        for (User user : deptUsers) {
            long userViolations = violationRepository.countByUser_Id(user.getId());
            if (userViolations >= 3) {
                repeatOffenderCount++;
            }
        }

        // Calculate department risk score (higher weight for aggregated issues)
        int riskScore = calculateDepartmentScore(
                lateAckCount, pendingViolations, deptAdminEscalations,
                superAdminEscalations, (int) totalLegalHoldImpact, safetyViolations, repeatOffenderCount
        );

        RiskLevel riskLevel = RiskScoreSnapshot.calculateRiskLevel(riskScore);

        // Compliance metrics
        long totalDocs = documentRepository.countByDepartmentId(departmentId);
        long acknowledgedCount = acknowledgementRepository.countByDepartmentId(departmentId);
        double complianceRate = totalDocs > 0 ?
                Math.min(100.0, (1.0 - (pendingViolations * 1.0 / Math.max(1, totalDocs))) * 100) : 100.0;

        // Trend data
        int violations7Days = (int) violationRepository.countViolationsSinceByDepartment(departmentId, last7Days);
        int violations30Days = (int) violationRepository.countViolationsSinceByDepartment(departmentId, last30Days);

        // Calculate trend percentage
        int previousPeriodViolations = totalViolations - violations30Days;
        int trendPercentage = previousPeriodViolations > 0 ?
                ((violations30Days - previousPeriodViolations) * 100 / previousPeriodViolations) : 0;
        String trendDirection = calculateTrend(violations7Days, violations30Days);

        // Count high-risk users
        int highRiskUserCount = 0;
        for (User user : deptUsers) {
            long userPendingViolations = violationRepository.countPendingByUserId(user.getId());
            if (userPendingViolations >= 2) {
                highRiskUserCount++;
            }
        }

        // Early warning indicators
        boolean hasRapidIncrease = violations7Days > 0 &&
                (violations30Days > 0 && (violations7Days * 4.0 / violations30Days) > 0.5);
        boolean hasMultipleEscalations = deptAdminEscalations + superAdminEscalations >= 3;
        boolean hasCriticalPending = deptViolations.stream()
                .anyMatch(this::isCriticalPending);

        return DepartmentRiskDTO.builder()
                .departmentId(departmentId)
                .departmentName(department.getName())
                .departmentCode(department.getCode())
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .riskLevelColor(getRiskLevelColor(riskLevel))
                .riskLevelDescription(getRiskLevelDescription(riskLevel))
                .lateAcknowledgementCount(lateAckCount)
                .totalViolationCount(totalViolations)
                .pendingViolationCount(pendingViolations)
                .resolvedViolationCount(totalViolations - pendingViolations)
                .deptAdminEscalationCount(deptAdminEscalations)
                .superAdminEscalationCount(superAdminEscalations)
                .legalHoldDocumentCount((int) legalHoldDocs)
                .safetyViolationCount(safetyViolations)
                .repeatOffenderCount(repeatOffenderCount)
                .complianceRate(complianceRate)
                .totalDocuments((int) totalDocs)
                .acknowledgedDocuments((int) acknowledgedCount)
                .pendingAcknowledgements(pendingViolations)
                .avgDaysDelayed(violationRepository.getAverageDaysDelayedByDepartment(departmentId))
                .violationsLast7Days(violations7Days)
                .violationsLast30Days(violations30Days)
                .violationsTrendPercentage(trendPercentage)
                .trendDirection(trendDirection)
                .totalUsers(deptUsers.size())
                .activeUsers((int) deptUsers.stream().filter(this::isActiveUser).count())
                .highRiskUserCount(highRiskUserCount)
                .lastCalculatedAt(now)
                .periodStart(now.minusDays(90))
                .periodEnd(now)
                .hasRapidViolationIncrease(hasRapidIncrease)
                .hasMultipleEscalations(hasMultipleEscalations)
                .hasCriticalPendingViolations(hasCriticalPending)
                .hasRepeatOffenders(repeatOffenderCount > 0)
                .build();
    }

    // ============================================
    // BATCH CALCULATIONS
    // ============================================

    

    @Transactional
    public List<RiskScoreSnapshot> calculateAndSaveAllUserRiskScores() {
        log.info("📊 Calculating risk scores for all users...");
        List<RiskScoreSnapshot> snapshots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            try {
                UserRiskDTO risk = calculateUserRisk(user.getId());
                RiskScoreSnapshot snapshot = createUserSnapshot(user, risk, now);
                snapshots.add(snapshotRepository.save(snapshot));
            } catch (Exception e) {
                log.error("❌ Failed to calculate risk for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("✅ Calculated risk scores for {} users", snapshots.size());
        return snapshots;
    }

    

    @Transactional
    public List<RiskScoreSnapshot> calculateAndSaveAllDepartmentRiskScores() {
        log.info("📊 Calculating risk scores for all departments...");
        List<RiskScoreSnapshot> snapshots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        List<Department> allDepartments = departmentRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        for (Department dept : allDepartments) {
            try {
                DepartmentRiskDTO risk = calculateDepartmentRisk(dept.getId());
                RiskScoreSnapshot snapshot = createDepartmentSnapshot(dept, risk, now);
                snapshots.add(snapshotRepository.save(snapshot));
            } catch (Exception e) {
                log.error("❌ Failed to calculate risk for department {}: {}", dept.getId(), e.getMessage());
            }
        }

        log.info("✅ Calculated risk scores for {} departments", snapshots.size());
        return snapshots;
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    

    private int calculateScore(int lateAcks, int pendingViolations, int deptEscalations,
                               int superEscalations, int legalHold, int safetyViolations, int repeatOffenses) {
        int score = 0;

        // Late acknowledgements
        score += Math.min(lateAcks * WEIGHT_LATE_ACK, MAX_LATE_ACK_POINTS);

        // Pending violations (most impactful)
        score += Math.min(pendingViolations * WEIGHT_PENDING_VIOLATION, MAX_PENDING_VIOLATION_POINTS);

        // Escalations
        score += Math.min(deptEscalations * WEIGHT_DEPT_ESCALATION, MAX_DEPT_ESCALATION_POINTS);
        score += Math.min(superEscalations * WEIGHT_SUPER_ESCALATION, MAX_SUPER_ESCALATION_POINTS);

        // Legal hold
        score += Math.min(legalHold * WEIGHT_LEGAL_HOLD, MAX_LEGAL_HOLD_POINTS);

        // Safety violations (highest weight)
        score += Math.min(safetyViolations * WEIGHT_SAFETY_VIOLATION, MAX_SAFETY_VIOLATION_POINTS);

        // Repeat offenses
        score += Math.min(repeatOffenses * WEIGHT_REPEAT_OFFENSE, MAX_REPEAT_OFFENSE_POINTS);

        return Math.min(100, score);  // Cap at 100
    }

    

    private int calculateDepartmentScore(int lateAcks, int pendingViolations, int deptEscalations,
                                          int superEscalations, int legalHold, int safetyViolations, int repeatOffenders) {
        int score = 0;

        // Scale factors for department (aggregated issues are weighted slightly less per incident)
        score += Math.min((int)(lateAcks * WEIGHT_LATE_ACK * 0.5), MAX_LATE_ACK_POINTS);
        score += Math.min((int)(pendingViolations * WEIGHT_PENDING_VIOLATION * 0.7), MAX_PENDING_VIOLATION_POINTS);
        score += Math.min(deptEscalations * WEIGHT_DEPT_ESCALATION, MAX_DEPT_ESCALATION_POINTS);
        score += Math.min(superEscalations * WEIGHT_SUPER_ESCALATION, MAX_SUPER_ESCALATION_POINTS);
        score += Math.min(legalHold * WEIGHT_LEGAL_HOLD, MAX_LEGAL_HOLD_POINTS);
        score += Math.min((int)(safetyViolations * WEIGHT_SAFETY_VIOLATION * 0.8), MAX_SAFETY_VIOLATION_POINTS);

        // Repeat offenders have high impact for department
        score += Math.min(repeatOffenders * 10, 30);

        return Math.min(100, score);
    }

    private String calculateTrend(int recent, int total) {
        if (total == 0) return "STABLE";
        double ratio = recent * 4.0 / total;  // Normalized to 30-day period
        if (ratio > 1.2) return "UP";
        if (ratio < 0.8) return "DOWN";
        return "STABLE";
    }

    private String getRiskLevelColor(RiskLevel level) {
        return switch (level) {
            case LOW -> "#22c55e";
            case MEDIUM -> "#f59e0b";
            case HIGH -> "#f97316";
            case CRITICAL -> "#ef4444";
        };
    }

    private String getRiskLevelDescription(RiskLevel level) {
        return switch (level) {
            case LOW -> "Risk within acceptable limits";
            case MEDIUM -> "Moderate risk - monitoring recommended";
            case HIGH -> "High risk - immediate attention required";
            case CRITICAL -> "Critical risk - urgent intervention needed";
        };
    }

    private RiskScoreSnapshot createUserSnapshot(User user, UserRiskDTO risk, LocalDateTime now) {
        return RiskScoreSnapshot.builder()
                .entityType(EntityType.USER)
                .entityId(user.getId())
                .entityName(user.getName())
                .riskScore(risk.getRiskScore())
                .riskLevel(risk.getRiskLevel())
                .lateAcknowledgementCount(risk.getLateAcknowledgementCount())
                .violationCount(risk.getTotalViolationCount())
                .pendingViolationCount(risk.getPendingViolationCount())
                .deptAdminEscalationCount(risk.getDeptAdminEscalationCount())
                .superAdminEscalationCount(risk.getSuperAdminEscalationCount())
                .legalHoldCount(risk.getLegalHoldDocumentCount())
                .safetyViolationCount(risk.getSafetyViolationCount())
                .repeatOffenseCount(Boolean.TRUE.equals(risk.getIsRepeatOffender()) ? 1 : 0)
                .calculationPeriodStart(now.minusDays(90))
                .calculationPeriodEnd(now)
                .calculationNotes("Automated calculation")
                .build();
    }

    private RiskScoreSnapshot createDepartmentSnapshot(Department dept, DepartmentRiskDTO risk, LocalDateTime now) {
        return RiskScoreSnapshot.builder()
                .entityType(EntityType.DEPARTMENT)
                .entityId(dept.getId())
                .entityName(dept.getName())
                .riskScore(risk.getRiskScore())
                .riskLevel(risk.getRiskLevel())
                .lateAcknowledgementCount(risk.getLateAcknowledgementCount())
                .violationCount(risk.getTotalViolationCount())
                .pendingViolationCount(risk.getPendingViolationCount())
                .deptAdminEscalationCount(risk.getDeptAdminEscalationCount())
                .superAdminEscalationCount(risk.getSuperAdminEscalationCount())
                .legalHoldCount(risk.getLegalHoldDocumentCount())
                .safetyViolationCount(risk.getSafetyViolationCount())
                .repeatOffenseCount(risk.getRepeatOffenderCount())
                .calculationPeriodStart(now.minusDays(90))
                .calculationPeriodEnd(now)
                .calculationNotes("Automated calculation")
                .build();
    }

    // ============================================
    // VIOLATION PREDICATE METHODS
    // ============================================

    private boolean isDeptAdminEscalated(ComplianceViolation v) {
        return Boolean.TRUE.equals(v.getDeptAdminEscalated());
    }

    private boolean isSuperAdminEscalated(ComplianceViolation v) {
        return Boolean.TRUE.equals(v.getSuperAdminEscalated());
    }

    private boolean isSafetyRelatedViolation(ComplianceViolation v) {
        return v.getDocument() != null &&
                (v.getDocument().getDocumentType() == DocumentType.SAFETY_CIRCULAR ||
                 v.getDocument().getDocumentType() == DocumentType.LEGAL_NOTICE);
    }

    private boolean isLegalHoldViolation(ComplianceViolation v) {
        return v.getDocument() != null && Boolean.TRUE.equals(v.getDocument().getLegalHold());
    }

    private boolean isOpenEscalation(ComplianceViolation v) {
        return !v.getResolved() &&
                (Boolean.TRUE.equals(v.getDeptAdminEscalated()) || Boolean.TRUE.equals(v.getSuperAdminEscalated()));
    }

    private boolean isCriticalPending(ComplianceViolation v) {
        return !v.getResolved() && v.getDaysDelayed() != null && v.getDaysDelayed() >= 14;
    }

    private boolean isActiveUser(User u) {
        return u.getIsActive();
    }
}
